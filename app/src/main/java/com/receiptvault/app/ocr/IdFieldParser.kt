package com.receiptvault.app.ocr

import com.receiptvault.app.domain.model.ParsedField
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdFieldParser @Inject constructor() {

    private val idPattern = Pattern.compile(
        """(?:id|license|licence|passport)\s*#?\s*[:.]?\s*([A-Z0-9-]{5,})""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(raw: String, lines: List<String>): IdParseResult {
        val nameLine = lines.firstOrNull { it.matches(Regex("""^[A-Za-z][A-Za-z\s'.-]{2,}$""")) }
        val idNum = idPattern.matcher(raw).let { if (it.find()) it.group(1) else null }
        val fields = buildList {
            nameLine?.let { add(ParsedField("name", "Name", it, 0.6f)) }
            idNum?.let { add(ParsedField("id", "ID #", it, 0.55f)) }
        }
        return IdParseResult(name = nameLine, idNumber = idNum, fields = fields)
    }
}

data class IdParseResult(
    val name: String?,
    val idNumber: String?,
    val fields: List<ParsedField>
)
