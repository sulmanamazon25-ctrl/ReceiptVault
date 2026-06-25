package com.receiptvault.app.domain.model

data class ScanResult(
    val imagePath: String,
    val extraPagePaths: List<String> = emptyList(),
    val documentType: DocumentType = DocumentType.RECEIPT,
    val ocrText: String = "",
    val scanConfidence: Float = 0f,
    val fields: List<ParsedField> = emptyList(),
    val merchantName: String? = null,
    val amount: Double? = null,
    val taxAmount: Double? = null,
    val dateMillis: Long? = null,
    val title: String? = null,
    val notes: String? = null
) {
    fun suggestedTitle(): String =
        title?.takeIf { it.isNotBlank() }
            ?: merchantName?.takeIf { it.isNotBlank() }
            ?: documentType.displayName
}
