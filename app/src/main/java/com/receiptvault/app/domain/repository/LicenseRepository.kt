package com.receiptvault.app.domain.repository

import com.receiptvault.app.domain.model.LicenseEntitlement
import kotlinx.coroutines.flow.Flow

interface LicenseRepository {
    fun observeLicense(): Flow<LicenseEntitlement?>
    suspend fun getLicense(): LicenseEntitlement?
    suspend fun activateLicense(licenseKey: String): Result<LicenseEntitlement>
    suspend fun validateOnline(): Result<Boolean>
    suspend fun clearLicense()
    fun observeLicenseActive(): Flow<Boolean>
}
