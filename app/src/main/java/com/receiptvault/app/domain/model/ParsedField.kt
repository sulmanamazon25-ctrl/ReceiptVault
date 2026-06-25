package com.receiptvault.app.domain.model

data class ParsedField(
    val key: String,
    val label: String,
    val value: String,
    val confidence: Float
)
