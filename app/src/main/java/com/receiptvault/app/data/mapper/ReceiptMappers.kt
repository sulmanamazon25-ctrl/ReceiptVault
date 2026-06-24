package com.receiptvault.app.data.mapper

import com.receiptvault.app.data.database.entity.FolderEntity
import com.receiptvault.app.data.database.entity.LicenseCacheEntity
import com.receiptvault.app.data.database.entity.ReceiptEntity
import com.receiptvault.app.data.database.entity.SubscriptionEntity
import com.receiptvault.app.domain.model.EntitlementSource
import com.receiptvault.app.domain.model.Folder
import com.receiptvault.app.domain.model.LicenseEntitlement
import com.receiptvault.app.domain.model.PurchaseType
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.model.Subscription

/** Pure mapping functions between Room entities and domain models. */

fun ReceiptEntity.toDomain(): Receipt = Receipt(
    id = id,
    title = title,
    merchantName = merchantName,
    amount = amount,
    taxAmount = taxAmount,
    date = date,
    category = category,
    notes = notes,
    folderId = folderId,
    imagePath = imagePath,
    pdfPath = pdfPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Receipt.toEntity(): ReceiptEntity = ReceiptEntity(
    id = id,
    title = title,
    merchantName = merchantName,
    amount = amount,
    taxAmount = taxAmount,
    date = date,
    category = category,
    notes = notes,
    folderId = folderId,
    imagePath = imagePath,
    pdfPath = pdfPath,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun FolderEntity.toDomain(): Folder = Folder(
    id = id,
    name = name,
    createdAt = createdAt
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    id = id,
    name = name,
    createdAt = createdAt
)

fun SubscriptionEntity.toDomain(): Subscription = Subscription(
    purchaseId = purchaseId,
    purchaseType = runCatching { PurchaseType.valueOf(purchaseType) }
        .getOrDefault(PurchaseType.NONE),
    purchaseDate = purchaseDate,
    expiryDate = expiryDate,
    source = runCatching { EntitlementSource.valueOf(source) }
        .getOrDefault(EntitlementSource.PLAY)
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    purchaseId = purchaseId,
    purchaseType = purchaseType.name,
    purchaseDate = purchaseDate,
    expiryDate = expiryDate,
    source = source.name
)

fun LicenseCacheEntity.toDomain(): LicenseEntitlement = LicenseEntitlement(
    licenseKeyId = licenseKeyId,
    tier = runCatching { PurchaseType.valueOf(tier) }
        .getOrDefault(PurchaseType.LIFETIME),
    token = token,
    tokenExpiresAt = tokenExpiresAt,
    lastValidatedAt = lastValidatedAt,
    deviceHash = deviceHash
)

fun LicenseEntitlement.toEntity(): LicenseCacheEntity = LicenseCacheEntity(
    licenseKeyId = licenseKeyId,
    tier = tier.name,
    token = token,
    tokenExpiresAt = tokenExpiresAt,
    lastValidatedAt = lastValidatedAt,
    deviceHash = deviceHash
)
