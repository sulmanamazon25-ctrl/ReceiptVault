package com.receiptvault.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.receiptvault.app.domain.model.DocumentType
import com.receiptvault.app.domain.model.ParsedField
import com.receiptvault.app.domain.model.ScanResult
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OcrPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val classifier: DocumentClassifier,
    private val receiptParser: ReceiptFieldParser,
    private val invoiceParser: InvoiceFieldParser,
    private val idParser: IdFieldParser
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun analyze(imageUri: Uri, imagePath: String): ScanResult {
        val image = InputImage.fromFilePath(context, imageUri)
        val visionText = recognizer.process(image).await()
        val raw = visionText.text
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
        val docType = classifier.classify(raw, lines.size)

        val fields = mutableListOf<ParsedField>()
        var merchant: String? = null
        var amount: Double? = null
        var tax: Double? = null
        var dateMillis: Long? = null
        var notes: String? = null
        var confidence = 0f

        when (docType) {
            DocumentType.RECEIPT, DocumentType.BILL, DocumentType.WARRANTY, DocumentType.OTHER -> {
                val parsed = receiptParser.parse(raw, lines)
                merchant = parsed.merchantName
                amount = parsed.amount
                tax = parsed.taxAmount
                dateMillis = parsed.dateMillis
                fields.addAll(parsed.fields)
                confidence = parsed.confidence
            }
            DocumentType.INVOICE -> {
                val receipt = receiptParser.parse(raw, lines)
                val invoice = invoiceParser.parse(raw, lines)
                merchant = invoice.vendorName ?: receipt.merchantName
                amount = receipt.amount
                tax = receipt.taxAmount
                dateMillis = receipt.dateMillis
                fields.addAll(receipt.fields + invoice.fields)
                confidence = receipt.confidence
                notes = invoice.invoiceNumber?.let { "Invoice #$it" }
            }
            DocumentType.ID_CARD, DocumentType.CONTRACT -> {
                val id = idParser.parse(raw, lines)
                merchant = id.name
                fields.addAll(id.fields)
                confidence = 0.5f
                notes = if (docType == DocumentType.CONTRACT) "Contract document" else "ID document"
            }
        }

        return ScanResult(
            imagePath = imagePath,
            documentType = docType,
            ocrText = raw,
            scanConfidence = confidence,
            fields = fields,
            merchantName = merchant,
            amount = amount,
            taxAmount = tax,
            dateMillis = dateMillis ?: System.currentTimeMillis(),
            title = merchant ?: docType.displayName,
            notes = notes
        )
    }

    fun fieldsToJson(fields: List<ParsedField>): String {
        val arr = JSONArray()
        fields.forEach { f ->
            arr.put(
                JSONObject()
                    .put("key", f.key)
                    .put("label", f.label)
                    .put("value", f.value)
                    .put("confidence", f.confidence.toDouble())
            )
        }
        return arr.toString()
    }

    fun jsonToFields(json: String?): List<ParsedField> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        ParsedField(
                            key = o.getString("key"),
                            label = o.getString("label"),
                            value = o.getString("value"),
                            confidence = o.getDouble("confidence").toFloat()
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
