package com.receiptvault.app.domain.model

/**
 * Domain representation of a stored document/receipt. Monetary values are nullable because a
 * receipt may be captured before its amount is parsed. Timestamps are epoch milliseconds.
 */
data class Receipt(
    val id: Long = 0L,
    val title: String,
    val merchantName: String? = null,
    val amount: Double? = null,
    val taxAmount: Double? = null,
    val date: Long,
    val category: String? = null,
    val notes: String? = null,
    val folderId: Long? = null,
    val imagePath: String? = null,
    val pdfPath: String? = null,
    val documentType: DocumentType = DocumentType.RECEIPT,
    val ocrText: String? = null,
    val scanConfidence: Float? = null,
    val extraPagesJson: String? = null,
    val parsedFieldsJson: String? = null,
    val isSensitive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun extraPagePaths(): List<String> {
        if (extraPagesJson.isNullOrBlank()) return emptyList()
        return runCatching {
            org.json.JSONArray(extraPagesJson).let { arr ->
                buildList {
                    for (i in 0 until arr.length()) add(arr.getString(i))
                }
            }
        }.getOrDefault(emptyList())
    }

    fun allImagePaths(): List<String> =
        listOfNotNull(imagePath) + extraPagePaths()
}
