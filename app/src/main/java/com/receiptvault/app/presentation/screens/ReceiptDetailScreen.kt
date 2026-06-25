package com.receiptvault.app.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.receiptvault.app.core.util.Formatters
import com.receiptvault.app.presentation.components.ConfirmDeleteDialog
import com.receiptvault.app.presentation.viewmodel.ReceiptDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptDetailScreen(
    onNavigateBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenSubscription: () -> Unit = {},
    viewModel: ReceiptDetailViewModel = hiltViewModel()
) {
    val receipt by viewModel.receipt.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val pdfUri by viewModel.exportUri.collectAsStateWithLifecycle()
    val exportError by viewModel.exportError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }

    LaunchedEffect(pdfUri) {
        pdfUri?.let { uri ->
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            context.startActivity(
                Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            viewModel.clearExportUri()
        }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("Receipt") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    receipt?.let { current ->
                        IconButton(onClick = { onEdit(current.id) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete")
                        }
                    }
                }
            )
        }
    ) { innerPadding: PaddingValues ->
        val current = receipt
        if (current == null) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Receipt not found.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val imagePath = current.imagePath
                if (imagePath != null) {
                    val bitmap = remember(imagePath) {
                        BitmapFactory.decodeFile(imagePath)?.asImageBitmap()
                    }
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Receipt photo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                        )
                    }
                }

                Text(
                    text = current.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = Formatters.formatCurrency(current.amount),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                DetailField(label = "Merchant", value = current.merchantName)
                DetailField(label = "Tax", value = current.taxAmount?.let(Formatters::formatCurrency))
                DetailField(label = "Date", value = Formatters.formatDate(current.date))
                DetailField(label = "Category", value = current.category)
                DetailField(label = "Notes", value = current.notes)

                OutlinedButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export document…")
                }
            }
        }
    }

    if (showExportSheet) {
        com.receiptvault.app.presentation.components.ExportBottomSheet(
            isPro = isPro,
            onDismiss = { showExportSheet = false },
            onExport = { format, toDownloads ->
                showExportSheet = false
                viewModel.export(format, toDownloads, isPro) { onOpenSubscription() }
            }
        )
    }

    exportError?.let {
        AlertDialog(
            onDismissRequest = { viewModel.clearExportError() },
            title = { Text("Export failed") },
            text = { Text(it) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearExportError() }) { Text("OK") }
            }
        )
    }

    if (showDeleteDialog && receipt != null) {
        ConfirmDeleteDialog(
            title = "Delete receipt?",
            text = "This permanently removes the receipt and its photo from this device.",
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete(onDeleted = onNavigateBack)
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun DetailField(label: String, value: String?) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
