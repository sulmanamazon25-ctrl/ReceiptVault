package com.receiptvault.app.presentation.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.billingclient.api.ProductDetails
import com.receiptvault.app.billing.BillingConnectionState
import com.receiptvault.app.billing.BillingManager
import com.receiptvault.app.domain.repository.LicenseRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingManager: BillingManager,
    private val licenseRepository: LicenseRepository,
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

    private val _licenseError = MutableStateFlow<String?>(null)
    val licenseError: StateFlow<String?> = _licenseError.asStateFlow()

    private val _isActivating = MutableStateFlow(false)
    val isActivating: StateFlow<Boolean> = _isActivating.asStateFlow()

    private val _licenseSuccess = MutableStateFlow(false)
    val licenseSuccess: StateFlow<Boolean> = _licenseSuccess.asStateFlow()

    init {
        billingManager.startConnection()
    }

    fun launchPurchase(activity: Activity, productDetails: ProductDetails) {
        billingManager.launchPurchase(activity, productDetails)
    }

    fun refreshPurchases() {
        billingManager.refreshPurchases()
    }

    fun activateLicense(key: String) {
        if (key.isBlank()) return
        viewModelScope.launch {
            _isActivating.value = true
            _licenseError.value = null
            licenseRepository.activateLicense(key.trim())
                .onSuccess {
                    _licenseSuccess.value = true
                }
                .onFailure { error ->
                    _licenseError.value = error.message ?: "Activation failed"
                }
            _isActivating.value = false
        }
    }

    fun clearLicenseMessages() {
        _licenseError.value = null
        _licenseSuccess.value = false
    }
}
