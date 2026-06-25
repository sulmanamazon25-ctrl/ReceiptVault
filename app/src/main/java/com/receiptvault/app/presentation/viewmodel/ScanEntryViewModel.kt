package com.receiptvault.app.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.domain.repository.SubscriptionRepository
import com.receiptvault.app.scan.ImportDocumentHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanEntryViewModel @Inject constructor(
    private val importHandler: ImportDocumentHandler,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val isPro = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    var isImporting by mutableStateOf(false)
        private set

    var importError by mutableStateOf<String?>(null)
        private set

    fun importAndReview(uri: Uri, onReady: (String) -> Unit) {
        viewModelScope.launch {
            isImporting = true
            importHandler.importUri(uri, applyEnhance = isPro.value)
                .onSuccess { onReady(it) }
                .onFailure { importError = it.message ?: "Import failed" }
            isImporting = false
        }
    }
}
