package com.receiptvault.app.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.domain.model.Folder
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.FolderRepository
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import com.receiptvault.app.pdf.ReportPdfExporter
import com.receiptvault.app.scanner.session.ScanJobQueue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    receiptRepository: ReceiptRepository,
    folderRepository: FolderRepository,
    private val reportPdfExporter: ReportPdfExporter,
    scanJobQueue: ScanJobQueue,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val scanJobsPending = scanJobQueue.jobs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allReceipts: StateFlow<List<Receipt>> = receiptRepository.observeReceipts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isPro: StateFlow<Boolean> = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _selectedFolderId = MutableStateFlow<Long?>(null)
    val selectedFolderId: StateFlow<Long?> = _selectedFolderId.asStateFlow()

    private val _reportUri = MutableStateFlow<Uri?>(null)
    val reportUri: StateFlow<Uri?> = _reportUri.asStateFlow()

    val receipts: StateFlow<List<Receipt>> = combine(
        allReceipts,
        selectedFolderId
    ) { list, folderId ->
        if (folderId == null) list else list.filter { it.folderId == folderId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectFolder(folderId: Long?) {
        _selectedFolderId.value = folderId
    }

    fun exportReport(isPro: Boolean, onNeedPro: () -> Unit) {
        if (!isPro) {
            onNeedPro()
            return
        }
        viewModelScope.launch {
            reportPdfExporter.exportReport(allReceipts.value)
                .onSuccess { _reportUri.value = it }
        }
    }

    fun clearReportUri() {
        _reportUri.value = null
    }
}
