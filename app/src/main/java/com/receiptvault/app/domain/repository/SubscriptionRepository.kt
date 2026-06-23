package com.receiptvault.app.domain.repository

import com.receiptvault.app.domain.model.Subscription
import kotlinx.coroutines.flow.Flow

/** Abstraction over the premium entitlement store. Implemented in the data layer. */
interface SubscriptionRepository {
    fun observeSubscription(): Flow<Subscription?>
    suspend fun getSubscription(): Subscription?
    suspend fun saveSubscription(subscription: Subscription)
    suspend fun clearSubscription()
    fun observeIsPro(): Flow<Boolean>
}
