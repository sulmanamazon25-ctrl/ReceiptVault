package com.receiptvault.app.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.receiptvault.app.data.database.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions LIMIT 1")
    fun observeActive(): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions LIMIT 1")
    suspend fun getActive(): SubscriptionEntity?

    @Upsert
    suspend fun upsert(subscription: SubscriptionEntity)

    @Query("DELETE FROM subscriptions")
    suspend fun clear()
}
