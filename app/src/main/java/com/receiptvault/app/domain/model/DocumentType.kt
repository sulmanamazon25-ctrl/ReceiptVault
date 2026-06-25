package com.receiptvault.app.domain.model

enum class DocumentType(val displayName: String) {
    RECEIPT("Receipt"),
    BILL("Bill"),
    INVOICE("Invoice"),
    WARRANTY("Warranty"),
    ID_CARD("ID"),
    CONTRACT("Contract"),
    OTHER("Other");

    val isSensitive: Boolean
        get() = this == ID_CARD || this == CONTRACT
}
