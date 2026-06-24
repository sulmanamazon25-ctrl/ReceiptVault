package com.receiptvault.app.data.repository

import com.receiptvault.app.data.database.dao.LicenseCacheDao
import com.receiptvault.app.data.database.dao.SubscriptionDao
import com.receiptvault.app.data.mapper.toDomain
import com.receiptvault.app.data.mapper.toEntity
import com.receiptvault.app.di.IoDispatcher
import com.receiptvault.app.domain.model.Subscription
import com.receiptvault.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    private val licenseCacheDao: LicenseCacheDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SubscriptionRepository {

    override fun observeSubscription(): Flow<Subscription?> =
        subscriptionDao.observeActive().map { entity -> entity?.toDomain() }

    override suspend fun getSubscription(): Subscription? = withContext(ioDispatcher) {
        subscriptionDao.getActive()?.toDomain()
    }

    override suspend fun saveSubscription(subscription: Subscription) = withContext(ioDispatcher) {
        subscriptionDao.upsert(subscription.toEntity())
    }

    override suspend fun clearSubscription() = withContext(ioDispatcher) {
        subscriptionDao.clear()
    }

    override fun observeIsPro(): Flow<Boolean> = combine(
        subscriptionDao.observeActive().map { it?.toDomain()?.isActive == true },
        licenseCacheDao.observe().map { it?.toDomain()?.isActive() == true }
    ) { playActive, licenseActive -> playActive || licenseActive }
}
