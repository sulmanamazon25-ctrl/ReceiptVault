package com.receiptvault.app.data.repository

import com.receiptvault.app.data.database.dao.FolderDao
import com.receiptvault.app.data.database.entity.FolderEntity
import com.receiptvault.app.data.mapper.toDomain
import com.receiptvault.app.data.mapper.toEntity
import com.receiptvault.app.di.IoDispatcher
import com.receiptvault.app.domain.model.Folder
import com.receiptvault.app.domain.repository.FolderRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FolderRepository {

    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { entities -> entities.map(FolderEntity::toDomain) }

    override suspend fun getFolder(id: Long): Folder? = withContext(ioDispatcher) {
        folderDao.getById(id)?.toDomain()
    }

    override suspend fun upsertFolder(folder: Folder): Long = withContext(ioDispatcher) {
        folderDao.upsert(folder.toEntity())
    }

    override suspend fun deleteFolder(folder: Folder) = withContext(ioDispatcher) {
        folderDao.delete(folder.toEntity())
    }

    override fun observeReceiptCount(folderId: Long): Flow<Int> =
        folderDao.observeReceiptCount(folderId)
}
