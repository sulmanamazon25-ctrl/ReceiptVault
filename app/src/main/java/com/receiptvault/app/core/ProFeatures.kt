package com.receiptvault.app.core

/** Pro feature flags and free-tier limits (from BuildConfig where applicable). */
object ProFeatures {
    const val FREE_MAX_FOLDERS = 5
    const val OFFLINE_GRACE_MS = 30L * 24 * 60 * 60 * 1000
    const val FREE_MAX_SCAN_PAGES = 1
}
