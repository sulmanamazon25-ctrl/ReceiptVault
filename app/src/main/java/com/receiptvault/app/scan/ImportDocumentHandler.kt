package com.receiptvault.app.scan

import android.content.Context
import android.net.Uri
import com.receiptvault.app.data.storage.ImageStorage
import com.receiptvault.app.scanner.ScannerProcessor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportDocumentHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imageStorage: ImageStorage,
    private val scannerProcessor: ScannerProcessor
) {
    suspend fun importUri(sourceUri: Uri, applyEnhance: Boolean): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = imageStorage.createImageFile()
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(raw).use { output -> input.copyTo(output) }
            } ?: throw IllegalStateException("Cannot read shared file")
            val out = imageStorage.createScanFile()
            scannerProcessor.processCapture(raw.absolutePath, out, applyEnhance).outputPath
        }
    }
}
