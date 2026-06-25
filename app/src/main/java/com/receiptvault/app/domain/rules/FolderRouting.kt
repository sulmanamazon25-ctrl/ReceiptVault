package com.receiptvault.app.domain.rules

import com.receiptvault.app.domain.model.DocumentType
import com.receiptvault.app.domain.model.Folder

object FolderRouting {

    private val defaultFolderNames = mapOf(
        DocumentType.RECEIPT to "Receipts",
        DocumentType.BILL to "Bills",
        DocumentType.INVOICE to "Invoices",
        DocumentType.WARRANTY to "Warranties",
        DocumentType.ID_CARD to "IDs",
        DocumentType.CONTRACT to "Contracts",
        DocumentType.OTHER to "Documents"
    )

    fun suggestedFolderName(documentType: DocumentType): String =
        defaultFolderNames[documentType] ?: "Documents"

    fun resolveFolderId(documentType: DocumentType, folders: List<Folder>): Long? {
        val targetName = suggestedFolderName(documentType)
        return folders.firstOrNull { it.name.equals(targetName, ignoreCase = true) }?.id
    }
}
