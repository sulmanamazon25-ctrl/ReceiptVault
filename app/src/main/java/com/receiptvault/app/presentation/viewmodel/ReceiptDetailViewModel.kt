package com.receiptvault.app.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val receiptId: Long =
        savedStateHandle.get<Long>(Screen.ReceiptDetail.ARG_RECEIPT_ID) ?: 0L

    val receipt: StateFlow<Receipt?> = receiptRepository.observeReceipt(receiptId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            receiptRepository.deleteReceiptById(receiptId)
            onDeleted()
        }
    }
}
