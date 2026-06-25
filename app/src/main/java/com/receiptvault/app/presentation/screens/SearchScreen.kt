package com.receiptvault.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.receiptvault.app.presentation.components.EmptyState
import com.receiptvault.app.presentation.components.ReceiptRow
import com.receiptvault.app.presentation.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val showReviewPrompt by viewModel.showReviewPrompt.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(results.size, query) {
        if (query.isNotBlank()) viewModel.onResultsDisplayed(results.size)
    }

    if (showReviewPrompt) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.dismissReviewPrompt() },
            title = { Text("Enjoying ReceiptVault?") },
            text = { Text("Help others find a private scanner — rate us on Google Play.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { viewModel.dismissReviewPrompt() }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                label = { Text(if (isPro) "Search titles & scanned text" else "Search by title (Pro: full text)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )

            when {
                query.isBlank() -> EmptyState(message = "Type to search your receipts.")
                results.isEmpty() -> EmptyState(message = "No receipts match \"$query\".")
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.id }) { receipt ->
                        ReceiptRow(
                            receipt = receipt,
                            onClick = { onOpenReceipt(receipt.id) }
                        )
                    }
                }
            }
        }
    }
}
