package com.receiptvault.app.domain

import com.receiptvault.app.domain.model.Receipt
import kotlin.math.abs

object DuplicateDetector {

    private const val AMOUNT_TOLERANCE = 0.01
    private const val DATE_TOLERANCE_MS = 86_400_000L

    fun findDuplicate(candidate: Receipt, existing: List<Receipt>): Receipt? {
        return existing.firstOrNull { other ->
            other.id != candidate.id &&
                amountsMatch(candidate.amount, other.amount) &&
                abs(candidate.date - other.date) <= DATE_TOLERANCE_MS &&
                merchantsMatch(candidate.merchantName, other.merchantName, candidate.title, other.title)
        }
    }

    private fun amountsMatch(a: Double?, b: Double?): Boolean {
        if (a == null || b == null) return false
        return abs(a - b) <= AMOUNT_TOLERANCE
    }

    private fun merchantsMatch(a: String?, b: String?, titleA: String, titleB: String): Boolean {
        val ma = a?.lowercase()?.trim().orEmpty()
        val mb = b?.lowercase()?.trim().orEmpty()
        if (ma.isNotBlank() && mb.isNotBlank()) {
            return ma == mb || ma.contains(mb) || mb.contains(ma)
        }
        return titleA.lowercase().trim() == titleB.lowercase().trim()
    }
}
