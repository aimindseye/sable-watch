package io.github.aimindseye.sablewatch.shared

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The top arc's Agenda Peek line, for example "11:00 AM • DESIGN REVIEW".
 *
 * Before the event starts it shows the start time, while it runs it shows "NOW", and once it
 * has ended it shows nothing. A private event shows "Busy" instead of its title.
 */
object AgendaPeek {
    private const val SEPARATOR = " • "
    const val BUSY = "BUSY"
    const val NOW = "NOW"
    const val ALL_DAY = "TODAY"

    /** The line at [at], or null when the event is over. */
    fun line(event: NextEvent, at: Instant, zone: ZoneId, use24Hour: Boolean): String? {
        val now = at.toEpochMilli()
        if (now >= event.endMillis && event.endMillis > event.beginMillis) return null
        val title = if (event.private) BUSY else event.title.trim().uppercase(Locale.getDefault())
        val whenText = when {
            event.allDay -> ALL_DAY

            now >= event.beginMillis -> NOW

            else -> {
                val pattern = if (use24Hour) "HH:mm" else "h:mm a"
                DateTimeFormatter.ofPattern(pattern, Locale.US)
                    .format(Instant.ofEpochMilli(event.beginMillis).atZone(zone))
            }
        }
        return whenText + SEPARATOR + title
    }

    /**
     * The moments the line changes: the event's start (time becomes "NOW") and its end (the
     * line goes away). The watch hands these to Wear OS as a timeline, so the arc changes on
     * time without waking the phone.
     */
    fun changes(event: NextEvent, after: Instant): List<Instant> = listOf(event.beginMillis, event.endMillis)
        .distinct()
        .map(Instant::ofEpochMilli)
        .filter { it.isAfter(after) }
        .sorted()
}
