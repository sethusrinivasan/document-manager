package com.app.paperstow.domain.safety

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GpxTrailTest {

    @Test
    fun `round trip keeps coordinates time and battery`() {
        val places = listOf(
            SafetyPlace(41.5, -72.5, 1_717_200_000_000, 84),
            SafetyPlace(41.501, -72.498, 1_717_200_600_000, 81)
        )
        val parsed = GpxTrail.parse(GpxTrail.toGpx(places))
        assertEquals(2, parsed.size)
        assertEquals(41.5, parsed[0].latitude, 0.00001)
        assertEquals(-72.5, parsed[0].longitude, 0.00001)
        assertEquals(84, parsed[0].batteryPercent)
        assertEquals(81, parsed[1].batteryPercent)
        assertEquals(places[0].timestamp, parsed[0].timestamp)
    }

    @Test
    fun `looksLikeGpx detects header`() {
        val bytes = GpxTrail.toGpx(emptyList()).toByteArray()
        assertTrue(GpxTrail.looksLikeGpx(bytes))
    }
}
