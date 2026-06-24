package com.receiptvault.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.receiptvault.app.data.database.entity.LicenseCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LicenseCacheDao {

    @Query("SELECT * FROM license_cache WHERE id = 1")
    fun observe(): Flow<LicenseCacheEntity?>

    @Query("SELECT * FROM license_cache WHERE id = 1")
    suspend fun get(): LicenseCacheEntity?

    @Upsert
    suspend fun upsert(entity: LicenseCacheEntity)

    @Query("DELETE FROM license_cache")
    suspend fun clear()
}
