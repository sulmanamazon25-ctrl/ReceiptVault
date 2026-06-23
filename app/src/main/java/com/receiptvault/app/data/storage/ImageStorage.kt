package com.receiptvault.app.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores captured receipt images inside the app's private internal storage. Files are exposed
 * to the system camera app only through a [FileProvider] content URI, so images never leave
 * the device and require no storage permission.
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR).apply { if (!exists()) mkdirs() }

    fun createImageFile(): File = File(imagesDir, "receipt_${System.currentTimeMillis()}.jpg")

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun deleteImage(path: String?) {
        if (!path.isNullOrBlank()) {
            runCatching { File(path).delete() }
        }
    }

    private companion object {
        const val IMAGES_DIR = "receipts"
    }
}
