package com.receiptvault.app.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import android.content.Intent
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.receiptvault.app.core.util.Formatters
import com.receiptvault.app.presentation.components.EmptyState
import com.receiptvault.app.presentation.components.ReceiptRow
import com.receiptvault.app.presentation.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onSmartScan: () -> Unit,
    onAddReceipt: () -> Unit,
    onOpenReceipt: (Long) -> Unit,
    onOpenFolders: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val receipts by viewModel.receipts.collectAsStateWithLifecycle()
    val scanJobs by viewModel.scanJobsPending.collectAsStateWithLifecycle()
    val pendingScans = scanJobs.count {
        it.status.name == "QUEUED" || it.status.name == "PROCESSING"
    }
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val selectedFolderId by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val reportUri by viewModel.reportUri.collectAsStateWithLifecycle()
    val total = receipts.sumOf { it.amount ?: 0.0 }

    LaunchedEffect(reportUri) {
        reportUri?.let { uri ->
            context.startActivity(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            viewModel.clearReportUri()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ReceiptVault") },
                actions = {
                    IconButton(onClick = onAddReceipt) {
                        Icon(Icons.Filled.Edit, contentDescription = "Add manually")
                    }
                    IconButton(onClick = {
                        viewModel.exportReport(isPro) { onOpenSubscription() }
                    }) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = "Export report PDF")
                    }
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = onOpenFolders) {
                        Icon(Icons.Filled.Folder, contentDescription = "Folders")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onSmartScan) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Smart Scan")
            }
        }
    ) { innerPadding: PaddingValues ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${receipts.size} documents",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (pendingScans > 0) {
                        Text(
                            text = "Processing $pendingScans scan(s)…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "Total: ${Formatters.formatCurrency(total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (folders.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFolderId == null,
                            onClick = { viewModel.selectFolder(null) },
                            label = { Text("All") }
                        )
                    }
                    items(folders, key = { it.id }) { folder ->
                        FilterChip(
                            selected = selectedFolderId == folder.id,
                            onClick = { viewModel.selectFolder(folder.id) },
                            label = { Text(folder.name) }
                        )
                    }
                }
            }

            if (receipts.isEmpty()) {
                EmptyState(message = "No documents yet. Tap the camera to Smart Scan.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(receipts, key = { it.id }) { receipt ->
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
