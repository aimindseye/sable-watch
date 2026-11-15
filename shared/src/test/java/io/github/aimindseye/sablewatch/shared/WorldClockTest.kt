package io.github.aimindseye.sablewatch.shared

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class WorldClockTest {
    private val nov9 = Instant.parse("2026-11-09T14:09:00Z")

    @Test
    fun `default cities match the design`() {
        assertEquals("LDN 14:09 • NYC 09:09 • TOK 23:09", WorldClock.line(WorldClock.DEFAULT_CITIES, nov9, true))
    }

    @Test
    fun `twelve hour clock has no leading zero`() {
        assertEquals("LDN 2:09 • NYC 9:09 • TOK 11:09", WorldClock.line(WorldClock.DEFAULT_CITIES, nov9, false))
    }

    @Test
    fun `daylight saving follows the city`() {
        val july = Instant.parse("2026-07-01T12:00:00Z")
        assertEquals("LDN 13:00 • NYC 08:00", WorldClock.line(WorldClock.DEFAULT_CITIES.take(2), july, true))
    }

    @Test
    fun `at most three cities are shown`() {
        val four = WorldClock.DEFAULT_CITIES + WorldCity("SYD", ZoneId.of("Australia/Sydney"))
        assertEquals(3, WorldClock.line(four, nov9, true).split(" • ").size)
    }

    @Test
    fun `short form is the first city`() {
        assertEquals("LDN 14:09", WorldClock.short(WorldClock.DEFAULT_CITIES, nov9, true))
    }
}
