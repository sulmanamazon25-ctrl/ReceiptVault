package com.receiptvault.app.core

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewPromptManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("review_prompt", Context.MODE_PRIVATE)

    fun shouldPromptAfterBatchScan(): Boolean {
        if (prefs.getBoolean(KEY_PROMPTED, false)) return false
        val count = prefs.getInt(KEY_BATCH_COUNT, 0) + 1
        prefs.edit { putInt(KEY_BATCH_COUNT, count) }
        return count >= 3
    }

    fun shouldPromptAfterSearchHit(): Boolean {
        if (prefs.getBoolean(KEY_PROMPTED, false)) return false
        return !prefs.getBoolean(KEY_SEARCH_HIT, false)
    }

    fun markSearchHit() {
        prefs.edit { putBoolean(KEY_SEARCH_HIT, true) }
    }

    fun markPrompted() {
        prefs.edit { putBoolean(KEY_PROMPTED, true) }
    }

    private companion object {
        const val KEY_PROMPTED = "prompted"
        const val KEY_BATCH_COUNT = "batch_count"
        const val KEY_SEARCH_HIT = "search_hit"
    }
}
