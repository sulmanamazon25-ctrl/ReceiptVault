package com.receiptvault.app.domain.model

/**
 * Domain representation of a stored receipt. Monetary values are nullable because a receipt
 * may be captured before its amount is parsed. Timestamps are epoch milliseconds.
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
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
