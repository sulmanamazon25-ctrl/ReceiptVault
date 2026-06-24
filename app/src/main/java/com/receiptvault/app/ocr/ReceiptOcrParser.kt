package com.receiptvault.app.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

data class OcrResult(
    val merchantName: String?,
    val amount: Double?,
    val rawText: String
)

@Singleton
class ReceiptOcrParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    private val amountPattern = Pattern.compile(
        """(?:total|amount|balance|due)\s*[:\$]?\s*(\d{1,6}(?:[.,]\d{2})?)""",
        Pattern.CASE_INSENSITIVE
    )
    private val currencyPattern = Pattern.compile("""\$?\s*(\d{1,6}[.,]\d{2})""")

    suspend fun parse(imageUri: Uri): Result<OcrResult> = runCatching {
        val image = InputImage.fromFilePath(context, imageUri)
        val visionText = recognizer.process(image).await()
        val raw = visionText.text
        val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

        val merchant = lines.firstOrNull { line ->
            line.length in 3..40 && !line.contains(Regex("""\d{2}[/.-]\d{2}"""))
        }

        val amount = findAmount(raw)

        OcrResult(
            merchantName = merchant,
            amount = amount,
            rawText = raw
        )
    }

    private fun findAmount(text: String): Double? {
        val matcher = amountPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(',', '.')?.toDoubleOrNull()
        }
        val amounts = currencyPattern.matcher(text).let { m ->
            generateSequence { if (m.find()) m.group(1) else null }.toList()
        }.mapNotNull { it.replace(',', '.').toDoubleOrNull() }
        return amounts.maxOrNull()
    }
}
