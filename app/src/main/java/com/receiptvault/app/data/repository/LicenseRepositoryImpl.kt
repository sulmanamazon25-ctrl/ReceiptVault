package com.receiptvault.app.data.repository

import com.receiptvault.app.BuildConfig
import com.receiptvault.app.data.database.dao.LicenseCacheDao
import com.receiptvault.app.data.mapper.toDomain
import com.receiptvault.app.data.mapper.toEntity
import com.receiptvault.app.di.IoDispatcher
import com.receiptvault.app.domain.model.LicenseEntitlement
import com.receiptvault.app.domain.model.PurchaseType
import com.receiptvault.app.domain.repository.LicenseRepository
import com.receiptvault.app.license.LicenseApi
import com.receiptvault.app.security.DeviceIdentity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseRepositoryImpl @Inject constructor(
    private val licenseCacheDao: LicenseCacheDao,
    private val licenseApi: LicenseApi,
    private val deviceIdentity: DeviceIdentity,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LicenseRepository {

    override fun observeLicense(): Flow<LicenseEntitlement?> =
        licenseCacheDao.observe().map { it?.toDomain() }

    override suspend fun getLicense(): LicenseEntitlement? = withContext(ioDispatcher) {
        licenseCacheDao.get()?.toDomain()
    }

    override suspend fun activateLicense(licenseKey: String): Result<LicenseEntitlement> =
        withContext(ioDispatcher) {
            val deviceHash = deviceIdentity.deviceHash()
            licenseApi.activate(
                licenseKey = licenseKey,
                deviceHash = deviceHash,
                deviceLabel = android.os.Build.MODEL,
                appVersion = BuildConfig.VERSION_NAME
            ).mapCatching { response ->
                val tier = when (response.tier) {
                    "monthly" -> PurchaseType.MONTHLY
                    "yearly" -> PurchaseType.YEARLY
                    else -> PurchaseType.LIFETIME
                }
                val now = System.currentTimeMillis()
                val entitlement = LicenseEntitlement(
                    licenseKeyId = response.licenseKeyId,
                    tier = tier,
                    token = response.token,
                    tokenExpiresAt = response.tokenExpiresAt,
                    lastValidatedAt = now,
                    deviceHash = deviceHash
                )
                licenseCacheDao.upsert(entitlement.toEntity())
                entitlement
            }
        }

    override suspend fun validateOnline(): Result<Boolean> = withContext(ioDispatcher) {
        val cached = licenseCacheDao.get()?.toDomain()
            ?: return@withContext Result.success(false)
        licenseApi.check(cached.licenseKeyId, cached.deviceHash).mapCatching { valid ->
            if (valid) {
                val updated = cached.copy(lastValidatedAt = System.currentTimeMillis())
                licenseCacheDao.upsert(updated.toEntity())
            } else {
                licenseCacheDao.clear()
            }
            valid
        }
    }

    override suspend fun clearLicense() = withContext(ioDispatcher) {
        licenseCacheDao.clear()
    }

    override fun observeLicenseActive(): Flow<Boolean> =
        observeLicense().map { it?.isActive() == true }
}
