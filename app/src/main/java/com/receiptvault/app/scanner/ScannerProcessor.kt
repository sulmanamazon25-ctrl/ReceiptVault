package com.receiptvault.app.scanner

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.receiptvault.app.scanner.detection.DocumentEdgeDetector
import com.receiptvault.app.scanner.enhance.ScanEnhancer
import com.receiptvault.app.scanner.model.DocumentCorners
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessedScan(
    val outputPath: String,
    val corners: DocumentCorners
)

@Singleton
class ScannerProcessor @Inject constructor(
    private val edgeDetector: DocumentEdgeDetector,
    private val enhancer: ScanEnhancer
) {
    fun processCapture(sourcePath: String, outputFile: File, applyEnhance: Boolean): ProcessedScan {
        val bitmap = BitmapFactory.decodeFile(sourcePath)
            ?: throw IllegalStateException("Could not decode capture")
        val corners = edgeDetector.detect(bitmap)
        val enhanced = if (applyEnhance) {
            val warped = enhancer.enhance(bitmap, corners)
            enhancer.compositeOnWhite(warped)
        } else {
            enhancer.warpPerspective(bitmap, corners)
        }
        FileOutputStream(outputFile).use { out ->
            enhanced.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        if (enhanced !== bitmap) bitmap.recycle()
        enhanced.recycle()
        File(sourcePath).delete()
        return ProcessedScan(outputPath = outputFile.absolutePath, corners = corners)
    }
}
