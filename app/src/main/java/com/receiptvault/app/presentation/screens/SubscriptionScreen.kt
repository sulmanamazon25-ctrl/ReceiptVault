package com.receiptvault.app.presentation.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.android.billingclient.api.ProductDetails
import com.receiptvault.app.billing.BillingConnectionState
import com.receiptvault.app.presentation.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onNavigateBack: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val productDetails by viewModel.productDetails.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ReceiptVault Pro") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (isPro) "You have ReceiptVault Pro" else "Premium features",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isPro) {
                    "Thank you for supporting ReceiptVault. Pro tools will unlock as they ship."
                } else {
                    "Everything in ReceiptVault is private and offline. Pro adds power-user tools. " +
                        "Subscriptions work when the app is installed from Google Play."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ProFeature("On-device OCR to auto-fill amounts and merchants")
            ProFeature("Export receipts and reports to PDF")
            ProFeature("Unlimited folders and advanced search filters")
            ProFeature("Encrypted local backups")

            if (!isPro) {
                when (connectionState) {
                    BillingConnectionState.CONNECTED -> {
                        if (productDetails.isEmpty()) {
                            Text(
                                text = "Products are not configured yet. Create pro_monthly, pro_yearly, " +
                                    "and pro_lifetime in Google Play Console.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            productDetails.forEach { details ->
                                ProductPurchaseRow(
                                    details = details,
                                    onPurchase = {
                                        val activity = context as? Activity ?: return@ProductPurchaseRow
                                        viewModel.launchPurchase(activity, details)
                                    }
                                )
                            }
                        }
                    }
                    BillingConnectionState.CONNECTING -> {
                        Text("Connecting to Google Play…", style = MaterialTheme.typography.bodySmall)
                    }
                    BillingConnectionState.ERROR, BillingConnectionState.DISCONNECTED -> {
                        Text(
                            text = "Play Billing is unavailable in sideloaded MVP builds. Install from " +
                                "Google Play when listed to subscribe.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        lastError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductPurchaseRow(
    details: ProductDetails,
    onPurchase: () -> Unit
) {
    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
        ?: details.subscriptionOfferDetails?.firstOrNull()
            ?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice
        ?: "—"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = details.name, style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(onClick = onPurchase) {
            Text(price)
        }
    }
}

@Composable
private fun ProFeature(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}
