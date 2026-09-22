package com.app.paperstow.domain.safety

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SafetyPlace(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val batteryPercent: Int = -1
)

/**
 * Unique places for My Trail: keep a new point only when the device
 * has moved far enough, and drop anything older than 24 hours.
 */
object UniqueLocations {
    const val MIN_SEPARATION_METERS = 150.0
    const val WINDOW_MS = 24L * 60 * 60 * 1000
    const val MAX_ACCURACY_METERS = 500f

    fun metersBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthMeters = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * earthMeters * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    fun isNewPlace(
        lastLat: Double,
        lastLon: Double,
        nextLat: Double,
        nextLon: Double,
        minMeters: Double = MIN_SEPARATION_METERS
    ): Boolean = metersBetween(lastLat, lastLon, nextLat, nextLon) >= minMeters

    fun inWindow(
        places: List<SafetyPlace>,
        nowMs: Long = System.currentTimeMillis(),
        windowMs: Long = WINDOW_MS
    ): List<SafetyPlace> = places.filter { nowMs - it.timestamp <= windowMs }

    fun mapsUrl(latitude: Double, longitude: Double): String =
        "https://maps.google.com/?q=$latitude,$longitude"

    fun shareAll(
        placesNewestFirst: List<SafetyPlace>,
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        if (placesNewestFirst.isEmpty()) {
            return "Paperstow — no unique places in My Trail on this phone."
        }
        val fmt = timeFormatter(locale, timeZone)
        val last = placesNewestFirst.first()
        val sb = StringBuilder()
        sb.appendLine("Paperstow — My Trail (this phone only)")
        sb.appendLine()
        sb.appendLine("Last known (${fmt.format(Date(last.timestamp))})${batterySuffix(last)}:")
        sb.appendLine(mapsUrl(last.latitude, last.longitude))
        val earlier = placesNewestFirst.drop(1)
        if (earlier.isNotEmpty()) {
            sb.appendLine()
            sb.appendLine("Earlier:")
            earlier.forEach { place ->
                sb.appendLine("• ${fmt.format(Date(place.timestamp))}${batterySuffix(place)} — ${mapsUrl(place.latitude, place.longitude)}")
            }
        }
        return sb.toString().trimEnd()
    }

    fun shareLastKnown(
        place: SafetyPlace,
        locale: Locale = Locale.getDefault(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): String {
        val fmt = timeFormatter(locale, timeZone)
        return buildString {
            appendLine("Paperstow — last known place")
            appendLine("${fmt.format(Date(place.timestamp))}${batterySuffix(place)}")
            append(mapsUrl(place.latitude, place.longitude))
        }.trimEnd()
    }

    fun batteryLabel(percent: Int): String? =
        if (percent in 0..100) "Battery $percent%" else null

    private fun batterySuffix(place: SafetyPlace): String {
        val label = batteryLabel(place.batteryPercent) ?: return ""
        return " · $label"
    }

    private fun timeFormatter(locale: Locale, timeZone: TimeZone): SimpleDateFormat {
        return SimpleDateFormat("h:mm a", locale).apply { this.timeZone = timeZone }
    }
}
