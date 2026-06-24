package com.receiptvault.app.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.receiptvault.app.BuildConfig
import com.receiptvault.app.presentation.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onOpenSubscription: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val appLock by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val screenshotProtection by viewModel.screenshotProtectionEnabled.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()
    val exportUri by viewModel.exportUri.collectAsStateWithLifecycle()
    var showExportDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var passphrase by remember { mutableStateOf("") }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null && passphrase.isNotBlank()) {
            viewModel.importBackup(uri, passphrase) { }
        }
    }

    LaunchedEffect(exportUri) {
        exportUri?.let { uri ->
            context.startActivity(
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            viewModel.clearBackupMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader("Security & privacy")

            SwitchRow(
                title = "Biometric app lock",
                subtitle = if (viewModel.biometricAvailable) {
                    "Require biometric unlock when opening the app"
                } else {
                    "Enroll a fingerprint or face unlock to enable this"
                },
                checked = appLock,
                enabled = viewModel.biometricAvailable,
                onCheckedChange = viewModel::setAppLockEnabled
            )
            SwitchRow(
                title = "Block screenshots",
                subtitle = "Hide receipts from screenshots and the recent-apps preview",
                checked = screenshotProtection,
                enabled = true,
                onCheckedChange = viewModel::setScreenshotProtectionEnabled
            )

            HorizontalDivider()
            SectionHeader("Appearance")

            SwitchRow(
                title = "Dynamic color",
                subtitle = "Match the app palette to your wallpaper (Android 12+)",
                checked = dynamicColor,
                enabled = true,
                onCheckedChange = viewModel::setDynamicColorEnabled
            )

            HorizontalDivider()
            SectionHeader("More")

            NavigationRow(
                title = "ReceiptVault Pro",
                subtitle = if (isPro) "Pro active" else "Unlock OCR, PDF, backup",
                onClick = onOpenSubscription
            )

            if (isPro) {
                SectionHeader("Pro tools")
                NavigationRow(
                    title = "Export encrypted backup",
                    subtitle = "Password-protected .rvbak file",
                    onClick = {
                        passphrase = ""
                        showExportDialog = true
                    }
                )
                NavigationRow(
                    title = "Restore from backup",
                    subtitle = "Replace local data from .rvbak",
                    onClick = {
                        passphrase = ""
                        showRestoreDialog = true
                    }
                )
                backupMessage?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            HorizontalDivider()
            SectionHeader("Legal & support")

            NavigationRow(
                title = "Privacy Policy",
                subtitle = "How your data is handled",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.PRIVACY_POLICY_URL))
                    )
                }
            )
            NavigationRow(
                title = "Terms of Service",
                subtitle = "License and usage terms",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.TERMS_URL))
                    )
                }
            )
            NavigationRow(
                title = "Contact support",
                subtitle = BuildConfig.SUPPORT_EMAIL,
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:${BuildConfig.SUPPORT_EMAIL}")
                        }
                    )
                }
            )

            HorizontalDivider()
            Text(
                text = "ReceiptVault ${BuildConfig.VERSION_NAME} — receipts stay on-device.\n© 2026 ReceiptVault",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export backup") },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Backup passphrase") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.exportBackup(passphrase)
                        showExportDialog = false
                    },
                    enabled = passphrase.length >= 8
                ) { Text("Export") }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore backup") },
            text = {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = { Text("Backup passphrase") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        restoreLauncher.launch(arrayOf("*/*"))
                        showRestoreDialog = false
                    },
                    enabled = passphrase.length >= 8
                ) { Text("Choose file") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun NavigationRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
