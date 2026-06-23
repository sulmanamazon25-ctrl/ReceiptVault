package com.receiptvault.app.security

import android.app.Activity
import android.view.WindowManager

/**
 * Toggles FLAG_SECURE on the activity window. When enabled, the OS blocks screenshots and
 * hides the window's content in the recent-apps switcher — a privacy safeguard for receipts
 * that may contain sensitive financial details.
 */
fun Activity.applyScreenshotProtection(enabled: Boolean) {
    if (enabled) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
