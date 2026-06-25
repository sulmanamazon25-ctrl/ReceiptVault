package com.receiptvault.app.ocr

import com.receiptvault.app.domain.model.DocumentType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentClassifier @Inject constructor() {

    fun classify(rawText: String, lineCount: Int): DocumentType {
        val lower = rawText.lowercase()
        return when {
            ID_KEYWORDS.any { lower.contains(it) } && lineCount <= 20 -> DocumentType.ID_CARD
            lower.contains("warranty") || lower.contains("guarantee") -> DocumentType.WARRANTY
            lower.contains("invoice") || lower.contains("invoice #") || lower.contains("invoice no") ->
                DocumentType.INVOICE
            lower.contains("bill") || lower.contains("statement") || lower.contains("amount due") ->
                DocumentType.BILL
            lower.contains("agreement") || lower.contains("contract") || lower.contains("party of the first") ->
                DocumentType.CONTRACT
            RECEIPT_KEYWORDS.any { lower.contains(it) } -> DocumentType.RECEIPT
            else -> DocumentType.OTHER
        }
    }

    private companion object {
        val ID_KEYWORDS = listOf("driver license", "driving licence", "passport", "national id", "identity card")
        val RECEIPT_KEYWORDS = listOf("receipt", "total", "subtotal", "thank you", "change", "visa", "mastercard")
    }
}
