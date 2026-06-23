package com.receiptvault.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.receiptvault.app.di.ApplicationScope
import com.receiptvault.app.di.IoDispatcher
import com.receiptvault.app.domain.model.PurchaseType
import com.receiptvault.app.domain.model.Subscription
import com.receiptvault.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Connects to Google Play Billing and syncs entitlements into [SubscriptionRepository]. */
@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext context: Context,
    private val subscriptionRepository: SubscriptionRepository,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher,
    @ApplicationScope private val applicationScope: CoroutineScope
) : PurchasesUpdatedListener {

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    private val _connectionState = MutableStateFlow(BillingConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BillingConnectionState> = _connectionState.asStateFlow()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    fun startConnection() {
        if (billingClient.isReady) {
            _connectionState.value = BillingConnectionState.CONNECTED
            refreshPurchases()
            queryProductDetails()
            return
        }
        _connectionState.value = BillingConnectionState.CONNECTING
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = BillingConnectionState.CONNECTED
                    refreshPurchases()
                    queryProductDetails()
                } else {
                    _connectionState.value = BillingConnectionState.ERROR
                    _lastError.value = result.debugMessage
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = BillingConnectionState.DISCONNECTED
            }
        })
    }

    fun queryProductDetails() {
        if (!billingClient.isReady) return

        val subscriptionParams = BillingProducts.ALL
            .filter { it != BillingProducts.PRO_LIFETIME }
            .map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

        val inAppParams = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(BillingProducts.PRO_LIFETIME)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(subscriptionParams + inAppParams)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = details
            } else {
                _lastError.value = result.debugMessage
            }
        }
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        if (!billingClient.isReady) {
            _lastError.value = "Billing service not ready"
            return
        }

        val params = when (productDetails.productType) {
            BillingClient.ProductType.SUBS -> {
                val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken
                    ?: run {
                        _lastError.value = "No subscription offer available"
                        return
                    }
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            }
            BillingClient.ProductType.INAPP -> {
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .build()
            }
            else -> {
                _lastError.value = "Unsupported product type"
                return
            }
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(params))
            .build()

        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) return

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { subsResult, subsPurchases ->
            if (subsResult.responseCode == BillingClient.BillingResponseCode.OK) {
                applicationScope.launch(ioDispatcher) {
                    subsPurchases.forEach { handlePurchase(it) }
                }
            }
        }

        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { inAppResult, inAppPurchases ->
            if (inAppResult.responseCode == BillingClient.BillingResponseCode.OK) {
                applicationScope.launch(ioDispatcher) {
                    inAppPurchases.forEach { handlePurchase(it) }
                }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            applicationScope.launch(ioDispatcher) {
                purchases.forEach { handlePurchase(it) }
            }
        } else if (result.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
            _lastError.value = result.debugMessage
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) { /* entitlement saved below */ }
        }

        val productId = purchase.products.firstOrNull() ?: return
        val purchaseType = when (productId) {
            BillingProducts.PRO_MONTHLY -> PurchaseType.MONTHLY
            BillingProducts.PRO_YEARLY -> PurchaseType.YEARLY
            BillingProducts.PRO_LIFETIME -> PurchaseType.LIFETIME
            else -> return
        }

        subscriptionRepository.saveSubscription(
            Subscription(
                purchaseId = purchase.purchaseToken,
                purchaseType = purchaseType,
                purchaseDate = purchase.purchaseTime,
                expiryDate = if (purchaseType == PurchaseType.LIFETIME) null else null
            )
        )
    }
}

enum class BillingConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}
