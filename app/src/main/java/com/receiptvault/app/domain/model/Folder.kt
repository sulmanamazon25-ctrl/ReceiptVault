package com.receiptvault.app.domain.model

/** Domain representation of a folder that groups receipts. */
data class Folder(
    val id: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)
