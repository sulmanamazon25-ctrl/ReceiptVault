package com.receiptvault.app.ocr

import com.receiptvault.app.domain.model.ParsedField
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceFieldParser @Inject constructor() {

    private val invoiceNumPattern = Pattern.compile(
        """(?:invoice\s*#?|inv\s*#?|invoice\s*no\.?)\s*[:#]?\s*([A-Z0-9-]+)""",
        Pattern.CASE_INSENSITIVE
    )
    private val dueDatePattern = Pattern.compile(
        """(?:due\s*date|payment\s*due)\s*[:\s]?\s*(\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4})""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(raw: String, lines: List<String>): InvoiceParseResult {
        val vendor = lines.firstOrNull { it.length in 3..50 }
        val invoiceNum = invoiceNumPattern.matcher(raw).let { if (it.find()) it.group(1) else null }
        val dueDate = dueDatePattern.matcher(raw).let { if (it.find()) it.group(1) else null }
        val fields = buildList {
            vendor?.let { add(ParsedField("vendor", "Vendor", it, 0.7f)) }
            invoiceNum?.let { add(ParsedField("invoice", "Invoice #", it, 0.8f)) }
            dueDate?.let { add(ParsedField("due", "Due date", it, 0.75f)) }
        }
        return InvoiceParseResult(
            vendorName = vendor,
            invoiceNumber = invoiceNum,
            dueDate = dueDate,
            fields = fields
        )
    }
}

data class InvoiceParseResult(
    val vendorName: String?,
    val invoiceNumber: String?,
    val dueDate: String?,
    val fields: List<ParsedField>
)
