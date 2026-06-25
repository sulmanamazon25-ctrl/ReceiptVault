package com.receiptvault.app.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.data.storage.ImageStorage
import com.receiptvault.app.domain.DuplicateDetector
import com.receiptvault.app.domain.model.DocumentType
import com.receiptvault.app.domain.model.ParsedField
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.model.ScanResult
import com.receiptvault.app.domain.repository.FolderRepository
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import com.receiptvault.app.domain.rules.FolderRouting
import com.receiptvault.app.ocr.OcrPipeline
import com.receiptvault.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

data class ScanReviewUiState(
    val imagePath: String = "",
    val extraPagePaths: List<String> = emptyList(),
    val documentType: DocumentType = DocumentType.RECEIPT,
    val title: String = "",
    val merchantName: String = "",
    val amount: String = "",
    val taxAmount: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val notes: String = "",
    val ocrText: String = "",
    val fields: List<ParsedField> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val duplicateWarning: String? = null,
    val isSensitive: Boolean = false
)

@HiltViewModel
class ScanReviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptRepository: ReceiptRepository,
    private val folderRepository: FolderRepository,
    private val imageStorage: ImageStorage,
    private val ocrPipeline: OcrPipeline,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val encodedPath: String = checkNotNull(savedStateHandle[Screen.ScanReview.ARG_IMAGE_PATH])

    val isPro = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    var uiState by mutableStateOf(ScanReviewUiState(imagePath = decodePath(encodedPath)))
        private set

    var saveError by mutableStateOf<String?>(null)
        private set

    init {
        runOcr()
    }

    private fun decodePath(encoded: String): String =
        java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())

    private fun runOcr() {
        viewModelScope.launch {
            val path = uiState.imagePath
            val uri = imageStorage.uriForFile(File(path))
            runCatching { ocrPipeline.analyze(uri, path) }
                .onSuccess { result -> applyScanResult(result) }
                .onFailure {
                    uiState = uiState.copy(isLoading = false)
                    saveError = it.message
                }
        }
    }

    private fun applyScanResult(result: ScanResult) {
        uiState = uiState.copy(
            isLoading = false,
            documentType = result.documentType,
            title = result.suggestedTitle(),
            merchantName = result.merchantName.orEmpty(),
            amount = result.amount?.toString().orEmpty(),
            taxAmount = result.taxAmount?.toString().orEmpty(),
            dateMillis = result.dateMillis ?: System.currentTimeMillis(),
            notes = result.notes.orEmpty(),
            ocrText = result.ocrText,
            fields = result.fields,
            isSensitive = result.documentType.isSensitive,
            extraPagePaths = result.extraPagePaths
        )
    }

    fun updateField(key: String, value: String) {
        val updated = uiState.fields.map {
            if (it.key == key) it.copy(value = value) else it
        }
        uiState = uiState.copy(fields = updated).also { syncFromFields(updated) }
    }

    fun updateTitle(v: String) { uiState = uiState.copy(title = v) }
    fun updateMerchant(v: String) { uiState = uiState.copy(merchantName = v) }
    fun updateAmount(v: String) { uiState = uiState.copy(amount = v) }
    fun updateTax(v: String) { uiState = uiState.copy(taxAmount = v) }
    fun updateNotes(v: String) { uiState = uiState.copy(notes = v) }
    fun updateDocumentType(type: DocumentType) {
        uiState = uiState.copy(
            documentType = type,
            isSensitive = type.isSensitive
        )
    }

    private fun syncFromFields(fields: List<ParsedField>) {
        var merchant = uiState.merchantName
        var amount = uiState.amount
        var tax = uiState.taxAmount
        fields.forEach { f ->
            when (f.key) {
                "merchant", "vendor" -> merchant = f.value
                "amount" -> amount = f.value
                "tax" -> tax = f.value
            }
        }
        uiState = uiState.copy(merchantName = merchant, amount = amount, taxAmount = tax)
    }

    fun save(isPro: Boolean, onSaved: (Long) -> Unit, onNeedPro: () -> Unit) {
        if (!isPro && uiState.extraPagePaths.isNotEmpty()) {
            onNeedPro()
            return
        }
        if (uiState.duplicateWarning != null) return
        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true)
            val state = uiState
            val now = System.currentTimeMillis()
            var folderId: Long? = null
            if (isPro) {
                val folders = folderRepository.observeFolders().first()
                folderId = FolderRouting.resolveFolderId(state.documentType, folders)
            }
            val candidate = Receipt(
                title = state.title.trim().ifBlank { state.documentType.displayName },
                merchantName = state.merchantName.trim().ifBlank { null },
                amount = state.amount.toDoubleOrNull(),
                taxAmount = state.taxAmount.toDoubleOrNull(),
                date = state.dateMillis,
                notes = state.notes.trim().ifBlank { null },
                folderId = folderId,
                imagePath = state.imagePath,
                documentType = state.documentType,
                ocrText = state.ocrText.ifBlank {
                    state.fields.joinToString("\n") { "${it.label}: ${it.value}" }
                },
                scanConfidence = state.fields.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() },
                extraPagesJson = encodePages(state.extraPagePaths),
                parsedFieldsJson = ocrPipeline.fieldsToJson(state.fields),
                isSensitive = state.isSensitive,
                createdAt = now,
                updatedAt = now
            )
            val existing = receiptRepository.observeReceipts().first()
            val dup = DuplicateDetector.findDuplicate(candidate, existing)
            if (dup != null && state.duplicateWarning == null) {
                uiState = state.copy(
                    isSaving = false,
                    duplicateWarning = "Similar document exists: ${dup.title}. Save anyway?"
                )
                return@launch
            }
            val id = receiptRepository.upsertReceipt(candidate)
            uiState = uiState.copy(isSaving = false)
            onSaved(id)
        }
    }

    fun confirmDuplicateSave(isPro: Boolean, onSaved: (Long) -> Unit) {
        val state = uiState.copy(duplicateWarning = null)
        uiState = state
        viewModelScope.launch {
            uiState = uiState.copy(isSaving = true)
            val now = System.currentTimeMillis()
            var folderId: Long? = null
            if (isPro) {
                val folders = folderRepository.observeFolders().first()
                folderId = FolderRouting.resolveFolderId(state.documentType, folders)
            }
            val candidate = Receipt(
                title = state.title.trim().ifBlank { state.documentType.displayName },
                merchantName = state.merchantName.trim().ifBlank { null },
                amount = state.amount.toDoubleOrNull(),
                taxAmount = state.taxAmount.toDoubleOrNull(),
                date = state.dateMillis,
                notes = state.notes.trim().ifBlank { null },
                folderId = folderId,
                imagePath = state.imagePath,
                documentType = state.documentType,
                ocrText = state.ocrText.ifBlank {
                    state.fields.joinToString("\n") { "${it.label}: ${it.value}" }
                },
                scanConfidence = state.fields.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() },
                extraPagesJson = encodePages(state.extraPagePaths),
                parsedFieldsJson = ocrPipeline.fieldsToJson(state.fields),
                isSensitive = state.isSensitive,
                createdAt = now,
                updatedAt = now
            )
            val id = receiptRepository.upsertReceipt(candidate)
            uiState = uiState.copy(isSaving = false)
            onSaved(id)
        }
    }

    fun dismissDuplicateWarning() {
        uiState = uiState.copy(duplicateWarning = null, isSaving = false)
    }

    private fun encodePages(paths: List<String>): String? {
        if (paths.isEmpty()) return null
        return JSONArray(paths).toString()
    }

    fun addPage(path: String) {
        uiState = uiState.copy(extraPagePaths = uiState.extraPagePaths + path)
        if (uiState.documentType == DocumentType.ID_CARD && uiState.extraPagePaths.size >= 1) {
            uiState = uiState.copy(isSensitive = true, notes = "ID card (front + back)")
        }
    }

    fun setIdCardMode() {
        uiState = uiState.copy(documentType = DocumentType.ID_CARD, isSensitive = true)
    }
}

private fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (isSuccess) action(getOrThrow())
    return this
}

private fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    exceptionOrNull()?.let(action)
    return this
}
