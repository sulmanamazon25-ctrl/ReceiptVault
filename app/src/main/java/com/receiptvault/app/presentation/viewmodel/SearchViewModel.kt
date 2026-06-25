package com.receiptvault.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptvault.app.core.ReviewPromptManager
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.domain.repository.ReceiptRepository
import com.receiptvault.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val receiptRepository: ReceiptRepository,
    subscriptionRepository: SubscriptionRepository,
    private val reviewPromptManager: ReviewPromptManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val isPro = subscriptionRepository.observeIsPro()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _showReviewPrompt = MutableStateFlow(false)
    val showReviewPrompt: StateFlow<Boolean> = _showReviewPrompt.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val results: StateFlow<List<Receipt>> = _query
        .flatMapLatest { text ->
            val trimmed = text.trim()
            if (trimmed.isEmpty()) {
                flowOf(emptyList())
            } else {
                search(trimmed, isPro.value)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun search(text: String, pro: Boolean): Flow<List<Receipt>> {
        return if (pro) {
            val fts = text.trim()
            if (fts.isBlank()) {
                receiptRepository.searchByTitle(text)
            } else {
                receiptRepository.searchFullText(fts)
            }
        } else {
            receiptRepository.searchByTitle(text)
        }
    }

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun onResultsDisplayed(resultCount: Int) {
        if (resultCount > 0 && _query.value.isNotBlank() && isPro.value) {
            reviewPromptManager.markSearchHit()
            if (reviewPromptManager.shouldPromptAfterSearchHit()) {
                _showReviewPrompt.value = true
            }
        }
    }

    fun dismissReviewPrompt() {
        _showReviewPrompt.value = false
        reviewPromptManager.markPrompted()
    }
}
