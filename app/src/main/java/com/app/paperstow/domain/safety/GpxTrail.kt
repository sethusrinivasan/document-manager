package com.app.paperstow.domain.safety

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * GPX 1.1 write/read for My Trail points. Keeps the file plain so a backup ZIP
 * can be unzipped and opened in any map app.
 */
object GpxTrail {
    fun toGpx(placesOldestFirst: List<SafetyPlace>, name: String = "My Trail"): String {
        val iso = isoFormatter()
        val body = StringBuilder()
        body.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        body.appendLine("<gpx version=\"1.1\" creator=\"Paperstow\" xmlns=\"http://www.topografix.com/GPX/1/1\">")
        body.appendLine("  <metadata>")
        body.appendLine("    <name>${escape(name)}</name>")
        body.appendLine("  </metadata>")
        body.appendLine("  <trk>")
        body.appendLine("    <name>${escape(name)}</name>")
        body.appendLine("    <trkseg>")
        placesOldestFirst.forEach { place ->
            body.appendLine(
                "      <trkpt lat=\"${place.latitude}\" lon=\"${place.longitude}\">"
            )
            body.appendLine("        <time>${iso.format(Date(place.timestamp))}</time>")
            if (place.batteryPercent in 0..100) {
                body.appendLine("        <cmt>battery ${place.batteryPercent}%</cmt>")
            }
            body.appendLine("      </trkpt>")
        }
        body.appendLine("    </trkseg>")
        body.appendLine("  </trk>")
        body.appendLine("</gpx>")
        return body.toString()
    }

    fun parse(xml: String): List<SafetyPlace> {
        val points = mutableListOf<SafetyPlace>()
        val point = Regex(
            """<(?:trkpt|wpt)\s+lat="([+-]?\d+(?:\.\d+)?)"\s+lon="([+-]?\d+(?:\.\d+)?)"\s*>(.*?)</(?:trkpt|wpt)>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
        )
        val timeTag = Regex("""<time>\s*([^<]+)\s*</time>""", RegexOption.IGNORE_CASE)
        val batteryTag = Regex("""battery\s+(\d{1,3})\s*%""", RegexOption.IGNORE_CASE)
        point.findAll(xml).forEach { match ->
            val lat = match.groupValues[1].toDoubleOrNull() ?: return@forEach
            val lon = match.groupValues[2].toDoubleOrNull() ?: return@forEach
            val inner = match.groupValues[3]
            val time = timeTag.find(inner)?.groupValues?.get(1)?.let { parseIso(it) } ?: 0L
            val battery = batteryTag.find(inner)?.groupValues?.get(1)?.toIntOrNull() ?: -1
            points += SafetyPlace(lat, lon, time, battery)
        }
        return points
    }

    fun looksLikeGpx(bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return false
        val head = String(bytes, 0, minOf(bytes.size, 800), Charsets.UTF_8)
        return head.contains("<gpx", ignoreCase = true)
    }

    private fun escape(value: String): String =
        value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;")

    private fun isoFormatter(): SimpleDateFormat =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

    private fun parseIso(value: String): Long {
        val cleaned = value.trim()
        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
        )
        for (pattern in patterns) {
            try {
                val fmt = SimpleDateFormat(pattern, Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                return fmt.parse(cleaned)?.time ?: continue
            } catch (_: Exception) {
                // try next pattern
            }
        }
        return 0L
    }
}
