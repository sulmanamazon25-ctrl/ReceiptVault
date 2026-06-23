package com.receiptvault.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room table for the premium entitlement. [purchaseType] stores a PurchaseType name. */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val purchaseId: String,
    val purchaseType: String,
    val purchaseDate: Long,
    val expiryDate: Long?
)
