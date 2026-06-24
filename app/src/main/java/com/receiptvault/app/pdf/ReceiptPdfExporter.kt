package com.receiptvault.app.pdf

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.receiptvault.app.BuildConfig
import com.receiptvault.app.core.util.Formatters
import com.receiptvault.app.domain.model.Receipt
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptPdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportReceipt(receipt: Receipt): Result<Uri> = runCatching {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 14f }
        var y = 48f

        canvas.drawText(receipt.title, 48f, y, titlePaint)
        y += 28f
        receipt.merchantName?.let {
            canvas.drawText("Merchant: $it", 48f, y, bodyPaint)
            y += 22f
        }
        receipt.amount?.let {
            canvas.drawText("Amount: ${Formatters.formatCurrency(it)}", 48f, y, bodyPaint)
            y += 22f
        }
        receipt.taxAmount?.let {
            canvas.drawText("Tax: ${Formatters.formatCurrency(it)}", 48f, y, bodyPaint)
            y += 22f
        }
        canvas.drawText("Date: ${Formatters.formatDate(receipt.date)}", 48f, y, bodyPaint)
        y += 22f
        receipt.category?.let {
            canvas.drawText("Category: $it", 48f, y, bodyPaint)
            y += 22f
        }
        receipt.notes?.let {
            canvas.drawText("Notes: $it", 48f, y, bodyPaint)
            y += 22f
        }

        receipt.imagePath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            if (bitmap != null) {
                val maxW = 500f
                val scale = maxW / bitmap.width
                val h = bitmap.height * scale
                if (y + h < 800f) {
                    canvas.drawBitmap(
                        bitmap,
                        null,
                        android.graphics.RectF(48f, y, 48f + maxW, y + h),
                        null
                    )
                }
                bitmap.recycle()
            }
        }

        document.finishPage(page)
        val outFile = File(context.cacheDir, "receipt_${receipt.id}.pdf")
        FileOutputStream(outFile).use { document.writeTo(it) }
        document.close()
        FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", outFile)
    }
}
