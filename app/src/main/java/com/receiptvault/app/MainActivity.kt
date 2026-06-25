package com.receiptvault.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.receiptvault.app.data.preferences.SettingsRepository
import com.receiptvault.app.domain.repository.LicenseRepository
import com.receiptvault.app.presentation.lock.LockScreen
import com.receiptvault.app.presentation.navigation.AppNavHost
import com.receiptvault.app.presentation.theme.ReceiptVaultTheme
import com.receiptvault.app.security.BiometricAuthenticator
import com.receiptvault.app.security.BiometricStatus
import com.receiptvault.app.security.applyScreenshotProtection
import com.receiptvault.app.scanner.session.BatchScanSession
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import javax.inject.Inject

/**
 * Single-activity host. Extends [FragmentActivity] because the AndroidX BiometricPrompt
 * requires it. Applies the user's privacy settings (biometric lock, screenshot protection,
 * dynamic color) reactively.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var biometricAuthenticator: BiometricAuthenticator

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var licenseRepository: LicenseRepository

    @Inject
    lateinit var batchScanSession: BatchScanSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            if (licenseRepository.getLicense() != null) {
                licenseRepository.validateOnline()
            }
        }
        enableEdgeToEdge()
        setContent {
            val dynamicColor by settingsRepository.dynamicColorEnabled.collectAsStateWithLifecycle()
            val screenshotProtection by settingsRepository.screenshotProtectionEnabled
                .collectAsStateWithLifecycle()
            val appLockEnabled by settingsRepository.appLockEnabled.collectAsStateWithLifecycle()

            LaunchedEffect(screenshotProtection) {
                this@MainActivity.applyScreenshotProtection(screenshotProtection)
            }

            ReceiptVaultTheme(dynamicColor = dynamicColor) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val lockRequired = appLockEnabled &&
                        biometricAuthenticator.status() == BiometricStatus.AVAILABLE
                    var unlocked by remember { mutableStateOf(false) }

                    if (lockRequired && !unlocked) {
                        LockScreen(onUnlock = { promptUnlock { unlocked = true } })
                        LaunchedEffect(Unit) {
                            promptUnlock { unlocked = true }
                        }
                    } else {
                        val navController = rememberNavController()
                        AppNavHost(
                            navController = navController,
                            batchScanSession = batchScanSession
                        )
                    }
                }
            }
        }
    }

    private fun promptUnlock(onSuccess: () -> Unit) {
        biometricAuthenticator.authenticate(
            activity = this,
            title = "Unlock ReceiptVault",
            subtitle = "Authenticate to access your receipts",
            onSuccess = onSuccess,
            onError = { _, _ -> },
            onFailed = { }
        )
    }
}
