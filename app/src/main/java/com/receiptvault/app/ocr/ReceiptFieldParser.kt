package com.receiptvault.app.ocr

import com.receiptvault.app.domain.model.ParsedField
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptFieldParser @Inject constructor() {

    private val amountPattern = Pattern.compile(
        """(?:total|amount|balance|due|grand\s*total)\s*[:\$]?\s*(\d{1,6}(?:[.,]\d{2})?)""",
        Pattern.CASE_INSENSITIVE
    )
    private val taxPattern = Pattern.compile(
        """(?:tax|vat|gst|hst)\s*[:\$]?\s*(\d{1,6}(?:[.,]\d{2})?)""",
        Pattern.CASE_INSENSITIVE
    )
    private val currencyPattern = Pattern.compile("""\$?\s*(\d{1,6}[.,]\d{2})""")
    private val datePatterns = listOf(
        Pattern.compile("""(\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4})"""),
        Pattern.compile("""(\d{4}[/.-]\d{1,2}[/.-]\d{1,2})""")
    )

    fun parse(raw: String, lines: List<String>): ReceiptParseResult {
        val merchant = lines.firstOrNull { line ->
            line.length in 3..45 &&
                !line.contains(Regex("""\d{2}[/.-]\d{2}""")) &&
                !line.contains(Regex("""^\d+$"""))
        }
        val amount = findAmount(raw)
        val tax = findTax(raw)
        val date = findDate(raw)
        val fields = buildList {
            merchant?.let { add(ParsedField("merchant", "Merchant", it, 0.75f)) }
            amount?.let { add(ParsedField("amount", "Total", "%.2f".format(it), 0.85f)) }
            tax?.let { add(ParsedField("tax", "Tax", "%.2f".format(it), 0.7f)) }
            date?.let { add(ParsedField("date", "Date", it, 0.65f)) }
        }
        return ReceiptParseResult(
            merchantName = merchant,
            amount = amount,
            taxAmount = tax,
            dateMillis = date?.let { parseDateMillis(it) },
            fields = fields,
            confidence = fields.map { it.confidence }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
        )
    }

    private fun findAmount(text: String): Double? {
        val matcher = amountPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(',', '.')?.toDoubleOrNull()
        }
        return currencyPattern.matcher(text).let { m ->
            generateSequence { if (m.find()) m.group(1) else null }.toList()
        }.mapNotNull { it.replace(',', '.').toDoubleOrNull() }.maxOrNull()
    }

    private fun findTax(text: String): Double? {
        val matcher = taxPattern.matcher(text)
        if (matcher.find()) {
            return matcher.group(1)?.replace(',', '.')?.toDoubleOrNull()
        }
        return null
    }

    private fun findDate(text: String): String? {
        for (pattern in datePatterns) {
            val m = pattern.matcher(text)
            if (m.find()) return m.group(1)
        }
        return null
    }

    private fun parseDateMillis(dateStr: String): Long? = runCatching {
        val formats = listOf("MM/dd/yyyy", "dd/MM/yyyy", "yyyy-MM-dd", "MM-dd-yyyy", "dd-MM-yyyy")
        for (fmt in formats) {
            val parsed = SimpleDateFormat(fmt, Locale.US).parse(dateStr)
            if (parsed != null) return parsed.time
        }
        null
    }.getOrNull()
}

data class ReceiptParseResult(
    val merchantName: String?,
    val amount: Double?,
    val taxAmount: Double?,
    val dateMillis: Long?,
    val fields: List<ParsedField>,
    val confidence: Float
)
