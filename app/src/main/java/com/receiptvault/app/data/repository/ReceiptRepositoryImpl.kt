package com.receiptvault.app.data.repository

import com.receiptvault.app.data.database.dao.ReceiptDao
import com.receiptvault.app.data.database.entity.ReceiptEntity
import com.receiptvault.app.data.mapper.toDomain
import com.receiptvault.app.data.mapper.toEntity
import com.receiptvault.app.di.IoDispatcher
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.ReceiptRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val receiptDao: ReceiptDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ReceiptRepository {

    override fun observeReceipts(): Flow<List<Receipt>> =
        receiptDao.observeAll().map { entities -> entities.map(ReceiptEntity::toDomain) }

    override fun observeReceiptsInFolder(folderId: Long?): Flow<List<Receipt>> =
        (if (folderId == null) receiptDao.observeUnfiled() else receiptDao.observeByFolder(folderId))
            .map { entities -> entities.map(ReceiptEntity::toDomain) }

    override fun observeReceipt(id: Long): Flow<Receipt?> =
        receiptDao.observeById(id).map { entity -> entity?.toDomain() }

    override suspend fun getReceipt(id: Long): Receipt? = withContext(ioDispatcher) {
        receiptDao.getById(id)?.toDomain()
    }

    override suspend fun upsertReceipt(receipt: Receipt): Long = withContext(ioDispatcher) {
        receiptDao.upsert(receipt.toEntity())
    }

    override suspend fun deleteReceipt(receipt: Receipt) = withContext(ioDispatcher) {
        receiptDao.delete(receipt.toEntity())
    }

    override suspend fun deleteReceiptById(id: Long) = withContext(ioDispatcher) {
        receiptDao.deleteById(id)
    }

    override fun searchByTitle(query: String): Flow<List<Receipt>> =
        receiptDao.searchByTitle(query).map { entities -> entities.map(ReceiptEntity::toDomain) }

    override fun searchByDateRange(start: Long, end: Long): Flow<List<Receipt>> =
        receiptDao.searchByDateRange(start, end)
            .map { entities -> entities.map(ReceiptEntity::toDomain) }

    override suspend fun count(): Int = withContext(ioDispatcher) {
        receiptDao.count()
    }

    override suspend fun totalAmount(): Double = withContext(ioDispatcher) {
        receiptDao.totalAmount()
    }
}
