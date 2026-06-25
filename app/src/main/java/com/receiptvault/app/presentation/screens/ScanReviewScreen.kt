package com.receiptvault.app.presentation.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.receiptvault.app.domain.model.DocumentType
import com.receiptvault.app.presentation.viewmodel.ScanReviewViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScanReviewScreen(
    onNavigateBack: () -> Unit,
    onSaved: (Long) -> Unit,
    onOpenSubscription: () -> Unit = {},
    onAddPage: () -> Unit = {},
    appendPagePath: String? = null,
    onAppendConsumed: () -> Unit = {},
    quickExit: Boolean = false,
    viewModel: ScanReviewViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()

    LaunchedEffect(appendPagePath) {
        appendPagePath?.let {
            viewModel.addPage(it)
            onAppendConsumed()
        }
    }

    if (state.duplicateWarning != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateWarning() },
            title = { Text("Possible duplicate") },
            text = { Text(state.duplicateWarning!!) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.confirmDuplicateSave(isPro) { id ->
                        if (quickExit) onNavigateBack() else onSaved(id)
                    }
                }) { Text("Save anyway") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicateWarning() }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review scan") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    viewModel.save(isPro, { id ->
                        if (quickExit) onNavigateBack() else onSaved(id)
                    }, onOpenSubscription)
                },
                enabled = !state.isLoading && !state.isSaving && state.title.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text(if (state.isSaving) "Saving…" else if (quickExit) "Save & return" else "Save to vault")
            }
        }
    ) { innerPadding: PaddingValues ->
        if (state.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(48.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                (listOfNotNull(state.imagePath.takeIf { File(it).exists() }) + state.extraPagePaths)
                    .forEachIndexed { index, path ->
                    if (File(path).exists()) {
                        BitmapFactory.decodeFile(path)?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Page ${index + 1}",
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                if (isPro) {
                    OutlinedButton(onClick = onAddPage, modifier = Modifier.fillMaxWidth()) {
                        Text("Add page (multi-page / ID back)")
                    }
                    OutlinedButton(
                        onClick = { viewModel.setIdCardMode() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Mark as ID card (sensitive)")
                    }
                }

                Text("Type: ${state.documentType.displayName}", style = MaterialTheme.typography.titleSmall)

                if (state.isSensitive) {
                    Text(
                        "Sensitive document — enable screenshot block in Settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DocumentType.entries.forEach { type ->
                        FilterChip(
                            selected = state.documentType == type,
                            onClick = { viewModel.updateDocumentType(type) },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                if (state.fields.isNotEmpty()) {
                    Text("Detected fields", style = MaterialTheme.typography.titleSmall)
                    state.fields.forEach { field ->
                        OutlinedTextField(
                            value = field.value,
                            onValueChange = { viewModel.updateField(field.key, it) },
                            label = { Text("${field.label} (${(field.confidence * 100).toInt()}%)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }

                OutlinedTextField(
                    value = state.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.merchantName,
                    onValueChange = viewModel::updateMerchant,
                    label = { Text("Merchant / vendor") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::updateAmount,
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = state.notes,
                    onValueChange = viewModel::updateNotes,
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
