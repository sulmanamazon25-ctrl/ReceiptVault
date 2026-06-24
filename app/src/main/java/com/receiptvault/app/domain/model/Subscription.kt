package com.receiptvault.app.domain.model

/** The tier of a purchase made through the billing flow or license key. */
enum class PurchaseType {
    NONE,
    MONTHLY,
    YEARLY,
    LIFETIME
}

/**
 * Domain representation of the user's premium entitlement.
 *
 * [isActive] treats a [PurchaseType.LIFETIME] purchase as permanently active, and any other
 * paid tier as active while its [expiryDate] is in the future.
 */
data class Subscription(
    val purchaseId: String,
    val purchaseType: PurchaseType,
    val purchaseDate: Long,
    val expiryDate: Long?,
    val source: EntitlementSource = EntitlementSource.PLAY
) {
    val isActive: Boolean
        get() = when (purchaseType) {
            PurchaseType.NONE -> false
            PurchaseType.LIFETIME -> true
            else -> expiryDate != null && expiryDate > System.currentTimeMillis()
        }
}
