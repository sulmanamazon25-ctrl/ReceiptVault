package com.receiptvault.app.scanner.mode

enum class ScanMode(val displayName: String, val requiresPro: Boolean) {
    SINGLE("Single", false),
    BATCH("Batch", true),
    MULTI_PAGE("Multi-page", true),
    ID_CARD("ID card", true),
    IMPORT("Import", false)
}
