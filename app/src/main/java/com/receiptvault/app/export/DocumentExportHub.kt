package com.receiptvault.app.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.receiptvault.app.BuildConfig
import com.receiptvault.app.domain.model.Receipt
import com.receiptvault.app.pdf.ReceiptPdfExporter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

enum class ExportFormat(val mimeType: String, val extension: String) {
    PDF("application/pdf", "pdf"),
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    TXT("text/plain", "txt"),
    ZIP("application/zip", "zip")
}

@Singleton
class DocumentExportHub @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfExporter: ReceiptPdfExporter
) {
    suspend fun export(
        receipt: Receipt,
        format: ExportFormat,
        isPro: Boolean,
        saveToDownloads: Boolean
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            when (format) {
                ExportFormat.PDF -> {
                    if (!isPro) error("Pro required for PDF export")
                    val uri = pdfExporter.exportReceipt(receipt).getOrThrow()
                    if (saveToDownloads) copyToDownloads(uri, "${safeName(receipt)}.pdf", "application/pdf")
                    uri
                }
                ExportFormat.JPEG, ExportFormat.PNG -> {
                    val path = receipt.imagePath ?: error("No image")
                    val bitmap = BitmapFactory.decodeFile(path) ?: error("Cannot decode image")
                    val compress = if (format == ExportFormat.JPEG) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG
                    val file = File(context.cacheDir, "${safeName(receipt)}.${format.extension}")
                    FileOutputStream(file).use { bitmap.compress(compress, 92, it) }
                    bitmap.recycle()
                    val uri = fileProviderUri(file)
                    if (saveToDownloads) copyToDownloads(uri, file.name, format.mimeType)
                    uri
                }
                ExportFormat.TXT -> {
                    if (!isPro) error("Pro required for text export")
                    val text = receipt.ocrText ?: receipt.title
                    val file = File(context.cacheDir, "${safeName(receipt)}.txt")
                    file.writeText(text)
                    val uri = fileProviderUri(file)
                    if (saveToDownloads) copyToDownloads(uri, file.name, format.mimeType)
                    uri
                }
                ExportFormat.ZIP -> {
                    if (!isPro) error("Pro required for ZIP export")
                    val zipFile = File(context.cacheDir, "${safeName(receipt)}.zip")
                    ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
                        receipt.allImagePaths().forEachIndexed { i, path ->
                            val f = File(path)
                            if (f.exists()) {
                                zip.putNextEntry(ZipEntry("page_${i + 1}.${f.extension.ifBlank { "jpg" }}"))
                                f.inputStream().use { it.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                        zip.putNextEntry(ZipEntry("manifest.json"))
                        val manifest = """{"title":"${receipt.title}","type":"${receipt.documentType.name}"}"""
                        zip.write(manifest.toByteArray())
                        zip.closeEntry()
                    }
                    val uri = fileProviderUri(zipFile)
                    if (saveToDownloads) copyToDownloads(uri, zipFile.name, format.mimeType)
                    uri
                }
            }
        }
    }

    private fun safeName(receipt: Receipt): String =
        receipt.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(40)

    private fun fileProviderUri(file: File): Uri =
        FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)

    private fun copyToDownloads(sourceUri: Uri, displayName: String, mimeType: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ReceiptVault")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val dest = resolver.insert(collection, values) ?: return sourceUri
        resolver.openOutputStream(dest)?.use { out ->
            resolver.openInputStream(sourceUri)?.use { input -> input.copyTo(out) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(dest, values, null, null)
        }
        return dest
    }
}
