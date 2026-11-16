package io.github.aimindseye.sablewatch.shared

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class WatchContractTest {
    private val full = WatchState(
        cities = listOf(WorldCity("BOM", ZoneId.of("Asia/Kolkata")), WorldCity("LDN", ZoneId.of("Europe/London"))),
        nextEvent = NextEvent("Design review", 1_000L, 2_000L, allDay = false, private = true),
        unread = 3,
        unreadSource = UnreadSource.HUB,
        weather = Weather(68, "F", "Cloudy", "Jersey City"),
        weatherSpot = WeatherSpot(40.7178, -74.0431, "Jersey City", fahrenheit = true),
        updatedAtMillis = 42L
    )

    @Test
    fun `round trip keeps everything`() {
        assertEquals(full, WatchContract.decode(WatchContract.encode(full)))
    }

    @Test
    fun `empty state round trips with default cities`() {
        val empty = WatchState()
        assertEquals(empty, WatchContract.decode(WatchContract.encode(empty)))
    }

    @Test
    fun `no message text is ever sent`() {
        val keys = WatchContract.encode(full).keys
        assertFalse(keys.any { "text" in it || "sender" in it || "preview" in it })
    }

    @Test
    fun `bad city entries are skipped`() {
        val map = mapOf(WatchContract.KEY_CITIES to listOf("XXX|Not/AZone", "nolabel", "LDN|Europe/London"))
        assertEquals(listOf(WorldCity("LDN", ZoneId.of("Europe/London"))), WatchContract.decode(map).cities)
    }

    @Test
    fun `missing unread stays unknown`() {
        assertNull(WatchContract.decode(emptyMap()).unread)
    }
}
