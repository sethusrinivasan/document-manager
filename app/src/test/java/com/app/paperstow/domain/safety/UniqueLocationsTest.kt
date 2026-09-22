package com.app.paperstow.domain.safety

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.TimeZone

class UniqueLocationsTest {

    @Test
    fun `same coordinates are not a new place`() {
        assertFalse(UniqueLocations.isNewPlace(10.0, 77.0, 10.0, 77.0))
    }

    @Test
    fun `one-degree latitude is far more than 150 meters`() {
        assertTrue(UniqueLocations.isNewPlace(0.0, 0.0, 1.0, 0.0))
    }

    @Test
    fun `about 150 meters of latitude counts as a new place`() {
        // Latitude-only haversine is R * Δrad; R matches UniqueLocations (6_371_000 m).
        val metersPerDegree = 6_371_000.0 * Math.PI / 180.0
        val delta = UniqueLocations.MIN_SEPARATION_METERS / metersPerDegree
        assertTrue(UniqueLocations.isNewPlace(0.0, 0.0, delta, 0.0))
        assertFalse(UniqueLocations.isNewPlace(0.0, 0.0, delta * 0.4, 0.0))
    }

    @Test
    fun `haversine is symmetric`() {
        val a = UniqueLocations.metersBetween(37.7749, -122.4194, 37.8044, -122.2711)
        val b = UniqueLocations.metersBetween(37.8044, -122.2711, 37.7749, -122.4194)
        assertEquals(a, b, 0.01)
        assertTrue(a > 10_000)
    }

    @Test
    fun `inWindow drops points older than 24 hours`() {
        val now = 1_700_000_000_000L
        val places = listOf(
            SafetyPlace(1.0, 2.0, now - 1_000),
            SafetyPlace(3.0, 4.0, now - UniqueLocations.WINDOW_MS - 1)
        )
        val kept = UniqueLocations.inWindow(places, now)
        assertEquals(1, kept.size)
        assertEquals(1.0, kept[0].latitude)
    }

    @Test
    fun `shareAll formats last known and earlier maps links`() {
        val noon = 1_704_110_400_000L // 2024-01-01 12:00 UTC
        val earlier = noon - 3 * 60 * 60 * 1000
        val text = UniqueLocations.shareAll(
            listOf(
                SafetyPlace(10.0, 20.0, noon),
                SafetyPlace(11.0, 21.0, earlier)
            ),
            locale = Locale.US,
            timeZone = TimeZone.getTimeZone("UTC")
        )
        assertTrue(text.contains("Last known (12:00 PM):"))
        assertTrue(text.contains("https://maps.google.com/?q=10.0,20.0"))
        assertTrue(text.contains("Earlier:"))
        assertTrue(text.contains("https://maps.google.com/?q=11.0,21.0"))
    }

    @Test
    fun `shareAll empty list is a clear message`() {
        val text = UniqueLocations.shareAll(emptyList())
        assertTrue(text.contains("no unique places"))
    }

    @Test
    fun `shareLastKnown is a short maps link`() {
        val noon = 1_704_110_400_000L
        val text = UniqueLocations.shareLastKnown(
            SafetyPlace(37.5, -122.1, noon),
            locale = Locale.US,
            timeZone = TimeZone.getTimeZone("UTC")
        )
        assertTrue(text.startsWith("Paperstow — last known place"))
        assertTrue(text.contains("https://maps.google.com/?q=37.5,-122.1"))
    }

    @Test
    fun `share includes battery when recorded`() {
        val noon = 1_704_110_400_000L
        val text = UniqueLocations.shareLastKnown(
            SafetyPlace(37.5, -122.1, noon, batteryPercent = 64),
            locale = Locale.US,
            timeZone = TimeZone.getTimeZone("UTC")
        )
        assertTrue(text.contains("Battery 64%"))
        assertEquals("Battery 64%", UniqueLocations.batteryLabel(64))
        assertEquals(null, UniqueLocations.batteryLabel(-1))
    }
}
