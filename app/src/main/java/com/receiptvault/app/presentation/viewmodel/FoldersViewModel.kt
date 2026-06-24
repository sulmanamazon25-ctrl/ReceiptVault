package com.receiptvault.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.core.ProFeatures
import com.receiptvault.app.domain.model.Folder
import com.receiptvault.app.domain.repository.FolderRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FoldersViewModel @Inject constructor(
    private val folderRepository: FolderRepository,
    subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    val folders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isPro: StateFlow<Boolean> = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun canCreateFolder(isPro: Boolean, currentCount: Int): Boolean =
        isPro || currentCount < ProFeatures.FREE_MAX_FOLDERS

    fun createFolder(name: String, isPro: Boolean, onLimitReached: () -> Unit) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (!canCreateFolder(isPro, folders.value.size)) {
            onLimitReached()
            return
        }
        viewModelScope.launch {
            folderRepository.upsertFolder(Folder(name = trimmed))
        }
    }

    fun deleteFolder(folder: Folder) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folder)
        }
    }
}
