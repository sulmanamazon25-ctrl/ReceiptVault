package com.receiptvault.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "license_cache")
data class LicenseCacheEntity(
    @PrimaryKey val id: Int = 1,
    val licenseKeyId: String,
    val tier: String,
    val token: String,
    val tokenExpiresAt: Long,
    val lastValidatedAt: Long,
    val deviceHash: String
)
