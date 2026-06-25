package com.receiptvault.app.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.receiptvault.app.export.ExportFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    isPro: Boolean,
    onDismiss: () -> Unit,
    onExport: (ExportFormat, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text("Export document", modifier = Modifier.padding(16.dp))
            HorizontalDivider()
            ExportRow("PDF", isPro) { onExport(ExportFormat.PDF, false) }
            ExportRow("Save PDF to Downloads", isPro) { onExport(ExportFormat.PDF, true) }
            ExportRow("JPEG (share)", true) { onExport(ExportFormat.JPEG, false) }
            ExportRow("PNG (share)", true) { onExport(ExportFormat.PNG, false) }
            ExportRow("Plain text (OCR)", isPro) { onExport(ExportFormat.TXT, false) }
            ExportRow("ZIP bundle", isPro) { onExport(ExportFormat.ZIP, false) }
        }
    }
}

@Composable
private fun ExportRow(label: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = if (enabled) label else "$label — Pro required",
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    )
}
