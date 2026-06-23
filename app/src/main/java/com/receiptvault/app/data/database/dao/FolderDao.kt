package com.receiptvault.app.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.receiptvault.app.data.database.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Upsert
    suspend fun upsert(folder: FolderEntity): Long

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("SELECT COUNT(*) FROM receipts WHERE folderId = :folderId")
    fun observeReceiptCount(folderId: Long): Flow<Int>
}
