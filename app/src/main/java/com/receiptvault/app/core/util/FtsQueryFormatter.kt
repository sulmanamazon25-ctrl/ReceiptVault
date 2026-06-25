package com.receiptvault.app.core.util

object FtsQueryFormatter {
    fun format(query: String): String =
        query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { token ->
                token.replace("\"", "").replace("*", "") + "*"
            }
}
