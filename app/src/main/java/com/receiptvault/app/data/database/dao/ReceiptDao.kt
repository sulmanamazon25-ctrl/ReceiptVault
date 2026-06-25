package com.receiptvault.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.receiptvault.app.data.database.entity.ReceiptEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReceiptDao {

    @Query("SELECT * FROM receipts ORDER BY date DESC")
    fun observeAll(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE folderId = :folderId ORDER BY date DESC")
    fun observeByFolder(folderId: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE folderId IS NULL ORDER BY date DESC")
    fun observeUnfiled(): Flow<List<ReceiptEntity>>

    @Query("SELECT * FROM receipts WHERE id = :id")
    fun observeById(id: Long): Flow<ReceiptEntity?>

    @Query("SELECT * FROM receipts WHERE id = :id")
    suspend fun getById(id: Long): ReceiptEntity?

    @Upsert
    suspend fun upsert(receipt: ReceiptEntity): Long

    @Delete
    suspend fun delete(receipt: ReceiptEntity)

    @Query("DELETE FROM receipts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM receipts WHERE title LIKE '%' || :query || '%' ORDER BY date DESC")
    fun searchByTitle(query: String): Flow<List<ReceiptEntity>>

    @Query(
        """
        SELECT * FROM receipts
        WHERE title LIKE '%' || :query || '%'
           OR ocrText LIKE '%' || :query || '%'
           OR merchantName LIKE '%' || :query || '%'
           OR notes LIKE '%' || :query || '%'
        ORDER BY date DESC
        """
    )
    fun searchFullText(query: String): Flow<List<ReceiptEntity>>

    @Query(
        """
        SELECT * FROM receipts
        WHERE amount IS NOT NULL AND ABS(amount - :amount) < 0.01
        AND ABS(date - :date) < :dateTolerance
        AND id != :excludeId
        LIMIT 5
        """
    )
    suspend fun findSimilar(amount: Double, date: Long, dateTolerance: Long, excludeId: Long): List<ReceiptEntity>

    @Query("SELECT * FROM receipts WHERE date BETWEEN :start AND :end ORDER BY date DESC")
    fun searchByDateRange(start: Long, end: Long): Flow<List<ReceiptEntity>>

    @Query("SELECT COUNT(*) FROM receipts")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM receipts")
    suspend fun totalAmount(): Double
}
