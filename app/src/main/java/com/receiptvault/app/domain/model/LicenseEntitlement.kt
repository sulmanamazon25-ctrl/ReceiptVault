package com.receiptvault.app.domain.model

import com.receiptvault.app.BuildConfig
import com.receiptvault.app.core.ProFeatures

/** Cached license activation from Supabase. */
data class LicenseEntitlement(
    val licenseKeyId: String,
    val tier: PurchaseType,
    val token: String,
    val tokenExpiresAt: Long,
    val lastValidatedAt: Long,
    val deviceHash: String
) {
    fun isActive(now: Long = System.currentTimeMillis()): Boolean {
        val graceEnd = lastValidatedAt + offlineGraceMs()
        return now <= tokenExpiresAt || now <= graceEnd
    }

    private fun offlineGraceMs(): Long =
        BuildConfig.LICENSE_OFFLINE_GRACE_DAYS.toLong() * 24 * 60 * 60 * 1000
}
