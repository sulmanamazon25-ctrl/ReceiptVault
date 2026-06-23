package com.receiptvault.app.domain.repository

import com.receiptvault.app.domain.model.Receipt
import kotlinx.coroutines.flow.Flow

/** Abstraction over receipt persistence. Implemented in the data layer. */
interface ReceiptRepository {
    fun observeReceipts(): Flow<List<Receipt>>
    fun observeReceiptsInFolder(folderId: Long?): Flow<List<Receipt>>
    fun observeReceipt(id: Long): Flow<Receipt?>
    suspend fun getReceipt(id: Long): Receipt?
    suspend fun upsertReceipt(receipt: Receipt): Long
    suspend fun deleteReceipt(receipt: Receipt)
    suspend fun deleteReceiptById(id: Long)
    fun searchByTitle(query: String): Flow<List<Receipt>>
    fun searchByDateRange(start: Long, end: Long): Flow<List<Receipt>>
    suspend fun count(): Int
    suspend fun totalAmount(): Double
}
