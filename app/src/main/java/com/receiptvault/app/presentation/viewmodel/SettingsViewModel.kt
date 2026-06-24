package com.receiptvault.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.backup.BackupExporter
import com.receiptvault.app.backup.BackupImporter
import com.receiptvault.app.data.preferences.SettingsRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import com.receiptvault.app.security.BiometricAuthenticator
import com.receiptvault.app.security.BiometricStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val biometricAuthenticator: BiometricAuthenticator,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val appLockEnabled: StateFlow<Boolean> = settingsRepository.appLockEnabled
    val screenshotProtectionEnabled: StateFlow<Boolean> = settingsRepository.screenshotProtectionEnabled
    val dynamicColorEnabled: StateFlow<Boolean> = settingsRepository.dynamicColorEnabled

    val isPro: StateFlow<Boolean> = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

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

    fun exportBackup(passphrase: String) {
        viewModelScope.launch {
            backupExporter.export(passphrase)
                .onSuccess { uri ->
                    _exportUri.value = uri
                    _backupMessage.value = "Backup ready to share"
                }
                .onFailure { _backupMessage.value = it.message ?: "Export failed" }
        }
    }

    fun importBackup(uri: Uri, passphrase: String, onDone: () -> Unit) {
        viewModelScope.launch {
            backupImporter.import(uri, passphrase)
                .onSuccess {
                    _backupMessage.value = "Restore complete — restart the app"
                    onDone()
                }
                .onFailure { _backupMessage.value = it.message ?: "Restore failed" }
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
        _exportUri.value = null
    }
}
