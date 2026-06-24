package com.receiptvault.app.pdf

import android.content.Context
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
class ReportPdfExporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun exportReport(receipts: List<Receipt>, title: String = "ReceiptVault Report"): Result<Uri> =
        runCatching {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas
            val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
            val bodyPaint = Paint().apply { textSize = 12f }
            var y = 48f

            canvas.drawText(title, 48f, y, titlePaint)
            y += 28f
            val total = receipts.sumOf { it.amount ?: 0.0 }
            canvas.drawText("${receipts.size} receipts — Total: ${Formatters.formatCurrency(total)}", 48f, y, bodyPaint)
            y += 28f

            receipts.take(30).forEach { receipt ->
                val line = buildString {
                    append(Formatters.formatDate(receipt.date))
                    append("  ")
                    append(receipt.merchantName ?: receipt.title)
                    append("  ")
                    append(Formatters.formatCurrency(receipt.amount ?: 0.0))
                }
                canvas.drawText(line.take(80), 48f, y, bodyPaint)
                y += 18f
                if (y > 780f) return@forEach
            }

            document.finishPage(page)
            val outFile = File(context.cacheDir, "receiptvault_report.pdf")
            FileOutputStream(outFile).use { document.writeTo(it) }
            document.close()
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", outFile)
        }
}
