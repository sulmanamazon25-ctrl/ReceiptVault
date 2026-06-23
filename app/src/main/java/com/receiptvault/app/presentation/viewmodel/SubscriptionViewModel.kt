package com.receiptvault.app.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.receiptvault.app.billing.BillingConnectionState
import com.receiptvault.app.billing.BillingManager
import com.receiptvault.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingManager: BillingManager,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val connectionState: StateFlow<BillingConnectionState> = billingManager.connectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BillingConnectionState.DISCONNECTED)

    val productDetails: StateFlow<List<ProductDetails>> = billingManager.productDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lastError: StateFlow<String?> = billingManager.lastError
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPro: StateFlow<Boolean> = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        billingManager.startConnection()
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchPurchase(activity, productDetails)
    }

    fun refreshPurchases() {
        billingManager.refreshPurchases()
    }
}
