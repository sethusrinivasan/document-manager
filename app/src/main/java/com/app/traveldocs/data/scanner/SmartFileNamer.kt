package com.app.traveldocs.data.scanner

import com.app.traveldocs.debug.DebugLogger
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartFileNamer @Inject constructor() {

    fun generateName(ocrText: String): String {
        if (ocrText.isBlank()) return fallbackName()

        val upper = ocrText.uppercase()
        val prefix = detectTypePrefix(upper)
        val identifier = extractIdentifier(ocrText, upper)

        val name = if (identifier.isNotBlank()) "${prefix}_$identifier" else prefix
        val sanitized = name.lowercase().replace(Regex("[^a-z0-9_]"), "_").replace(Regex("_+"), "_").trim('_').take(60)
        val result = "$sanitized.jpg"
        DebugLogger.d("SmartNamer", "Generated: $result from ${ocrText.take(50)}...")
        return result
    }

    private fun detectTypePrefix(upper: String): String = when {
        upper.contains("PASSPORT") || upper.contains("PASSEPORT") -> "passport"
        upper.contains("VISA") -> "visa"
        upper.contains("TICKET") || upper.contains("BOOKING") || upper.contains("BOARDING") || upper.contains("FLIGHT") -> "ticket"
        upper.contains("HOTEL") || upper.contains("RESERVATION") || upper.contains("CHECK-IN") -> "hotel"
        upper.contains("INSURANCE") || upper.contains("POLICY") || upper.contains("COVERAGE") -> "insurance"
        else -> "document"
    }

    private fun extractIdentifier(text: String, upper: String): String {
        // Try name
        val nameMatch = Regex("(?:surname|name|holder)[/:\\s]*([A-Z][a-zA-Z\\s-]{2,20})", RegexOption.IGNORE_CASE).find(text)
        if (nameMatch != null) {
            val name = nameMatch.groupValues[1].trim().replace(" ", "_").take(20)
            if (name.length > 2) return name
        }

        // Try destination
        val destMatch = Regex("(?:to|destination)[:\\s]+([A-Z][a-zA-Z]+)", RegexOption.IGNORE_CASE).find(text)
        if (destMatch != null) return destMatch.groupValues[1].take(15)

        // Try booking ref
        val refMatch = Regex("(?:booking|PNR|ref|confirmation)[.:\\s]*([A-Z0-9]{5,8})", RegexOption.IGNORE_CASE).find(text)
        if (refMatch != null) return refMatch.groupValues[1]

        // Try passport/visa number
        val idMatch = Regex("(?:passport|visa)\\s*(?:no|number|#)?[.:\\s]*([A-Z0-9]{6,9})", RegexOption.IGNORE_CASE).find(text)
        if (idMatch != null) return idMatch.groupValues[1]

        // Fallback: timestamp
        return SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    }

    private fun fallbackName(): String {
        return "document_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg"
    }
}
