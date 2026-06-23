package com.receiptvault.app.data.preferences

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists user preferences in private SharedPreferences and exposes them as observable
 * [StateFlow]s so the UI reacts to changes immediately.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _screenshotProtectionEnabled =
        MutableStateFlow(prefs.getBoolean(KEY_SCREENSHOT_PROTECTION, true))
    val screenshotProtectionEnabled: StateFlow<Boolean> = _screenshotProtectionEnabled.asStateFlow()

    private val _dynamicColorEnabled = MutableStateFlow(prefs.getBoolean(KEY_DYNAMIC_COLOR, true))
    val dynamicColorEnabled: StateFlow<Boolean> = _dynamicColorEnabled.asStateFlow()

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APP_LOCK, enabled).apply()
        _appLockEnabled.value = enabled
    }

    fun setScreenshotProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCREENSHOT_PROTECTION, enabled).apply()
        _screenshotProtectionEnabled.value = enabled
    }

    fun setDynamicColorEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColorEnabled.value = enabled
    }

    private companion object {
        const val PREFS_NAME = "receiptvault_settings"
        const val KEY_APP_LOCK = "app_lock_enabled"
        const val KEY_SCREENSHOT_PROTECTION = "screenshot_protection_enabled"
        const val KEY_DYNAMIC_COLOR = "dynamic_color_enabled"
    }
}
