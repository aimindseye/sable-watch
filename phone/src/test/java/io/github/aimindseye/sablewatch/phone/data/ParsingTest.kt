package io.github.aimindseye.sablewatch.phone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsingTest {
    private val hour = 3_600_000L
    private val now = 1_000 * hour

    private fun row(
        title: String,
        begin: Long,
        end: Long = begin + hour,
        allDay: Boolean = false,
        private: Boolean = false
    ) = CalendarRow(title, begin, end, allDay, private)

    @Test
    fun picksTheEarliestEventNotOverYet() {
        val rows = listOf(row("Later", now + 2 * hour), row("Past", now - 2 * hour), row("Soon", now + hour))
        assertEquals("Soon", Parsing.pickNext(rows, now, 4, skipAllDay = true, hideTitles = false)?.title)
    }

    @Test
    fun keepsAnEventThatIsOnNow() {
        val rows = listOf(row("Standup", now - 10 * 60_000L), row("Next", now + hour))
        assertEquals("Standup", Parsing.pickNext(rows, now, 4, skipAllDay = true, hideTitles = false)?.title)
    }

    @Test
    fun leavesOutEventsBeyondTheWindow() {
        assertNull(Parsing.pickNext(listOf(row("Far", now + 5 * hour)), now, 4, skipAllDay = true, hideTitles = false))
    }

    @Test
    fun skipsAllDayEventsWhenAsked() {
        val rows = listOf(row("Holiday", now - hour, now + 20 * hour, allDay = true), row("Call", now + hour))
        assertEquals("Call", Parsing.pickNext(rows, now, 4, skipAllDay = true, hideTitles = false)?.title)
        assertEquals("Holiday", Parsing.pickNext(rows, now, 4, skipAllDay = false, hideTitles = false)?.title)
    }

    @Test
    fun marksPrivateHiddenAndUntitledEvents() {
        assertTrue(Parsing.pickNext(listOf(row("Doctor", now + hour, private = true)), now, 4, true, false)!!.private)
        assertTrue(Parsing.pickNext(listOf(row("Review", now + hour)), now, 4, true, hideTitles = true)!!.private)
        assertTrue(Parsing.pickNext(listOf(row(" ", now + hour)), now, 4, true, false)!!.private)
    }

    @Test
    fun readsSableWeatherDetail() {
        val weather = Parsing.weather("New York · 21° · Clear", "LIVE")!!
        assertEquals(21, weather.temperature)
        assertEquals("Clear", weather.condition)
        assertEquals("New York", weather.place)
    }

    @Test
    fun readsNegativeTemperatures() {
        assertEquals(-4, Parsing.weather("Oslo · -4° · Snow", "STALE")!!.temperature)
    }

    @Test
    fun noWeatherWhenSableWeatherHasNothing() {
        assertNull(Parsing.weather("Open Sable Weather to set it up", "UNAVAILABLE"))
        assertNull(Parsing.weather("Updating", "LOADING"))
        assertNull(Parsing.weather(null, null))
    }

    @Test
    fun totalsCountsAndKeepsNoRowsApart() {
        assertEquals(5, Parsing.total(listOf(2, 3, -1)))
        assertEquals(0, Parsing.total(emptyList()))
        assertNull(Parsing.total(null))
    }

    @Test
    fun defaultEnds() {
        assertEquals(now + hour, Parsing.defaultEnd(now, allDay = false))
        assertEquals(now + 24 * hour, Parsing.defaultEnd(now, allDay = true))
    }
}
