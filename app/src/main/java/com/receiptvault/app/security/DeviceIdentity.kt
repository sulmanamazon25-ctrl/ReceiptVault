package com.receiptvault.app.security

import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Stable per-device hash for license binding (ANDROID_ID + app salt). */
@Singleton
class DeviceIdentity @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val salt = "receiptvault-v2-device-salt"

    fun deviceHash(): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$androidId:$salt".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
