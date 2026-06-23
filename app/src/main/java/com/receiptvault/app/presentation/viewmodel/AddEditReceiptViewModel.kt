package com.receiptvault.app.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.data.storage.ImageStorage
import com.receiptvault.app.domain.model.Folder
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.FolderRepository
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Immutable form state for the add/edit screen. */
data class AddEditReceiptUiState(
    val title: String = "",
    val merchantName: String = "",
    val amount: String = "",
    val taxAmount: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val category: String = "",
    val notes: String = "",
    val folderId: Long? = null,
    val imagePath: String? = null,
    val isEditMode: Boolean = false
) {
    val canSave: Boolean get() = title.isNotBlank()
}

@HiltViewModel
class AddEditReceiptViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptRepository: ReceiptRepository,
    folderRepository: FolderRepository,
    private val imageStorage: ImageStorage
) : ViewModel() {

    private val receiptId: Long =
        savedStateHandle.get<Long>(Screen.AddEditReceipt.ARG_RECEIPT_ID)
            ?: Screen.AddEditReceipt.NO_RECEIPT

    private val isEditMode: Boolean = receiptId != Screen.AddEditReceipt.NO_RECEIPT

    private var createdAt: Long = System.currentTimeMillis()
    private var pendingImagePath: String? = null

    var uiState by mutableStateOf(AddEditReceiptUiState(isEditMode = isEditMode))
        private set

    val folders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (isEditMode) {
            viewModelScope.launch {
                receiptRepository.getReceipt(receiptId)?.let { receipt ->
                    createdAt = receipt.createdAt
                    uiState = uiState.copy(
                        title = receipt.title,
                        merchantName = receipt.merchantName.orEmpty(),
                        amount = receipt.amount?.toString().orEmpty(),
                        taxAmount = receipt.taxAmount?.toString().orEmpty(),
                        dateMillis = receipt.date,
                        category = receipt.category.orEmpty(),
                        notes = receipt.notes.orEmpty(),
                        folderId = receipt.folderId,
                        imagePath = receipt.imagePath
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) { uiState = uiState.copy(title = value) }
    fun onMerchantChange(value: String) { uiState = uiState.copy(merchantName = value) }
    fun onAmountChange(value: String) { uiState = uiState.copy(amount = value) }
    fun onTaxChange(value: String) { uiState = uiState.copy(taxAmount = value) }
    fun onDateChange(millis: Long) { uiState = uiState.copy(dateMillis = millis) }
    fun onCategoryChange(value: String) { uiState = uiState.copy(category = value) }
    fun onNotesChange(value: String) { uiState = uiState.copy(notes = value) }
    fun onFolderSelected(folderId: Long?) { uiState = uiState.copy(folderId = folderId) }

    /** Creates the target file for a new capture and returns the content URI to hand the camera. */
    fun prepareImageCapture(): Uri {
        val file = imageStorage.createImageFile()
        pendingImagePath = file.absolutePath
        return imageStorage.uriForFile(file)
    }

    fun onImageCaptured() {
        val previous = uiState.imagePath
        if (previous != null && previous != pendingImagePath) {
            imageStorage.deleteImage(previous)
        }
        uiState = uiState.copy(imagePath = pendingImagePath)
        pendingImagePath = null
    }

    fun onCaptureCancelled() {
        imageStorage.deleteImage(pendingImagePath)
        pendingImagePath = null
    }

    fun removeImage() {
        imageStorage.deleteImage(uiState.imagePath)
        uiState = uiState.copy(imagePath = null)
    }

    fun save(onSaved: () -> Unit) {
        val state = uiState
        if (state.title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val receipt = Receipt(
                id = if (isEditMode) receiptId else 0L,
                title = state.title.trim(),
                merchantName = state.merchantName.trim().ifBlank { null },
                amount = state.amount.toDoubleOrNull(),
                taxAmount = state.taxAmount.toDoubleOrNull(),
                date = state.dateMillis,
                category = state.category.trim().ifBlank { null },
                notes = state.notes.trim().ifBlank { null },
                folderId = state.folderId,
                imagePath = state.imagePath,
                pdfPath = null,
                createdAt = if (isEditMode) createdAt else now,
                updatedAt = now
            )
            receiptRepository.upsertReceipt(receipt)
            onSaved()
        }
    }
}
