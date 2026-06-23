package com.receiptvault.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.domain.model.Folder
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.FolderRepository
import com.receiptvault.app.domain.repository.ReceiptRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    receiptRepository: ReceiptRepository,
    folderRepository: FolderRepository
) : ViewModel() {

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    val folders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val receipts: StateFlow<List<Receipt>> = _selectedFolderId
        .flatMapLatest { folderId ->
            if (folderId == null) {
                receiptRepository.observeReceipts()
            } else {
                receiptRepository.observeReceiptsInFolder(folderId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
