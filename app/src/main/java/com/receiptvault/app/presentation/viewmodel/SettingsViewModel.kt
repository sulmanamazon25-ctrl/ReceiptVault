package com.receiptvault.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.receiptvault.app.data.preferences.SettingsRepository
import com.receiptvault.app.security.BiometricAuthenticator
import com.receiptvault.app.security.BiometricStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean> = settingsRepository.appLockEnabled
    val screenshotProtectionEnabled: StateFlow<Boolean> = settingsRepository.screenshotProtectionEnabled
    val dynamicColorEnabled: StateFlow<Boolean> = settingsRepository.dynamicColorEnabled

    /** App lock can only be turned on when the device actually has strong biometrics enrolled. */
    val biometricAvailable: Boolean
        get() = biometricAuthenticator.status() == BiometricStatus.AVAILABLE

    fun setAppLockEnabled(enabled: Boolean) {
        if (enabled && !biometricAvailable) return
        settingsRepository.setAppLockEnabled(enabled)
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        settingsRepository.setScreenshotProtectionEnabled(enabled)
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        settingsRepository.setDynamicColorEnabled(enabled)
    }
}
