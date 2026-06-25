package com.receiptvault.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import com.receiptvault.app.export.DocumentExportHub
import com.receiptvault.app.export.ExportFormat
import com.receiptvault.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptRepository: ReceiptRepository,
    private val exportHub: DocumentExportHub,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val receiptId: Long =
        checkNotNull(savedStateHandle.get<Long>(Screen.ReceiptDetail.ARG_RECEIPT_ID))

    private val _receipt = MutableStateFlow<Receipt?>(null)
    val receipt: StateFlow<Receipt?> = _receipt.asStateFlow()

    val isPro: StateFlow<Boolean> = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _exportUri = MutableStateFlow<Uri?>(null)
    val exportUri: StateFlow<Uri?> = _exportUri.asStateFlow()

    private val _exportError = MutableStateFlow<String?>(null)
    val exportError: StateFlow<String?> = _exportError.asStateFlow()

    init {
        viewModelScope.launch {
            _receipt.value = receiptRepository.getReceipt(receiptId)
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            receiptRepository.getReceipt(receiptId)?.let { receiptRepository.deleteReceipt(it) }
            onDeleted()
        }
    }

    fun export(format: ExportFormat, saveToDownloads: Boolean, isPro: Boolean, onNeedPro: () -> Unit) {
        val receipt = _receipt.value ?: return
        val needsPro = format != ExportFormat.JPEG && format != ExportFormat.PNG
        if (needsPro && !isPro) {
            onNeedPro()
            return
        }
        viewModelScope.launch {
            exportHub.export(receipt, format, isPro, saveToDownloads)
                .onSuccess { _exportUri.value = it }
                .onFailure { _exportError.value = it.message }
        }
    }

    fun clearExportUri() {
        _exportUri.value = null
    }

    fun clearExportError() {
        _exportError.value = null
    }
}
