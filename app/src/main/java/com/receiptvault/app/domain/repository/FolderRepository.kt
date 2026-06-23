package com.receiptvault.app.domain.repository

import com.receiptvault.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

/** Abstraction over folder persistence. Implemented in the data layer. */
interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>
    suspend fun getFolder(id: Long): Folder?
    suspend fun upsertFolder(folder: Folder): Long
    suspend fun deleteFolder(folder: Folder)
    fun observeReceiptCount(folderId: Long): Flow<Int>
}
