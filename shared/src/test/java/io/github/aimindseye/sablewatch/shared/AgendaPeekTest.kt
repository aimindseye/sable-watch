package io.github.aimindseye.sablewatch.shared

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgendaPeekTest {
    private val ny = ZoneId.of("America/New_York")
    private val begin = Instant.parse("2026-11-09T16:00:00Z") // 11:00 AM in New York
    private val event =
        NextEvent("Design review", begin.toEpochMilli(), begin.plusSeconds(3600).toEpochMilli(), false, false)

    @Test
    fun `before the start it shows the time`() {
        assertEquals("11:00 AM • DESIGN REVIEW", AgendaPeek.line(event, begin.minusSeconds(600), ny, false))
        assertEquals("11:00 • DESIGN REVIEW", AgendaPeek.line(event, begin.minusSeconds(600), ny, true))
    }

    @Test
    fun `while it runs it shows now`() {
        assertEquals("NOW • DESIGN REVIEW", AgendaPeek.line(event, begin.plusSeconds(60), ny, false))
    }

    @Test
    fun `after the end it shows nothing`() {
        assertNull(AgendaPeek.line(event, begin.plusSeconds(3600), ny, false))
    }

    @Test
    fun `private events hide the title`() {
        assertEquals("11:00 AM • BUSY", AgendaPeek.line(event.copy(private = true), begin.minusSeconds(1), ny, false))
    }

    @Test
    fun `all day events say today`() {
        assertEquals(
            "TODAY • DESIGN REVIEW",
            AgendaPeek.line(event.copy(allDay = true), begin.minusSeconds(1), ny, false)
        )
    }

    @Test
    fun `changes are the start and the end, in the future only`() {
        assertEquals(listOf(begin, begin.plusSeconds(3600)), AgendaPeek.changes(event, begin.minusSeconds(1)))
        assertEquals(listOf(begin.plusSeconds(3600)), AgendaPeek.changes(event, begin))
    }
}
