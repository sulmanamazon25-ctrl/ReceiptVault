package com.receiptvault.app.billing

/** Product IDs configured in Google Play Console (Subscriptions + one-time). */
object BillingProducts {
    const val PRO_MONTHLY = "pro_monthly"
    const val PRO_YEARLY = "pro_yearly"
    const val PRO_LIFETIME = "pro_lifetime"

    val ALL = listOf(PRO_MONTHLY, PRO_YEARLY, PRO_LIFETIME)
}
