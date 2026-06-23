package com.receiptvault.app.core.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Formatting helpers shared across the UI layer. */
object Formatters {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

    fun formatDate(epochMillis: Long): String = dateFormat.format(Date(epochMillis))

    fun formatCurrency(amount: Double?): String {
        if (amount == null) return "—"
        return NumberFormat.getCurrencyInstance().format(amount)
    }
}
