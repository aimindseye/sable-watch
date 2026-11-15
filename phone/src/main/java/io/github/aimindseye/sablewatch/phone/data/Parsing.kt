package io.github.aimindseye.sablewatch.phone.data

import io.github.aimindseye.sablewatch.shared.NextEvent
import io.github.aimindseye.sablewatch.shared.Weather

/** A row read from a calendar, before the user's Agenda Peek options are applied. */
data class CalendarRow(
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val private: Boolean
)

/** Pure choices on what was read, kept apart from Android so they are unit-tested. */
object Parsing {
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 24 * HOUR_MS

    /** The end of an event a source gave no end for: a day for all-day events, else an hour. */
    fun defaultEnd(begin: Long, allDay: Boolean): Long = begin + if (allDay) DAY_MS else HOUR_MS

    /**
     * The event Agenda Peek shows: the first one not over yet that starts within [windowHours],
     * leaving out all-day events when [skipAllDay]. With [hideTitles], every event is "Busy".
     */
    fun pickNext(
        rows: List<CalendarRow>,
        now: Long,
        windowHours: Int,
        skipAllDay: Boolean,
        hideTitles: Boolean
    ): NextEvent? = rows
        .asSequence()
        .filter { it.endMillis > now && it.beginMillis <= now + windowHours * HOUR_MS }
        .filterNot { skipAllDay && it.allDay }
        .sortedWith(compareBy<CalendarRow> { it.beginMillis }.thenBy { it.allDay })
        .firstOrNull()
        ?.let {
            NextEvent(
                title = it.title.trim(),
                beginMillis = it.beginMillis,
                endMillis = it.endMillis,
                allDay = it.allDay,
                private = hideTitles || it.private || it.title.isBlank()
            )
        }

    private val TEMPERATURE = Regex("""(-?\d+)\s*°\s*([CF])?""")

    /**
     * Reads Sable Weather's detail line, "Place · 21° · Clear". Null when it holds no temperature,
     * which is how Sable Weather says it has nothing current (the line is then a message).
     */
    fun weather(detail: String?, availability: String?): Weather? {
        if (detail.isNullOrBlank() || availability.equals("UNAVAILABLE", ignoreCase = true)) return null
        val parts = detail.split(" · ").map(String::trim)
        val tempIndex = parts.indexOfFirst { TEMPERATURE.containsMatchIn(it) }
        if (tempIndex < 0) return null
        val match = TEMPERATURE.find(parts[tempIndex]) ?: return null
        return Weather(
            temperature = match.groupValues[1].toInt(),
            unit = match.groupValues[2],
            condition = parts.getOrNull(tempIndex + 1).orEmpty(),
            place = parts.take(tempIndex).joinToString(" ")
        )
    }

    /** The total of package/count rows; null when there were no rows to read. */
    fun total(counts: List<Int>?): Int? = counts?.sumOf { it.coerceAtLeast(0) }
}
