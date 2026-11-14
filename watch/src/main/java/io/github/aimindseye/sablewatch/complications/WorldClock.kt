package io.github.aimindseye.sablewatch.complications

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** A city on the top arc: a short label such as "LDN" and its time zone. */
data class WorldCity(val label: String, val zone: ZoneId)

/**
 * Formats the world clock line for the top arc, for example "LDN 14:09 • NYC 09:09 • TOK 23:09".
 *
 * The cities' times are computed on the watch from their time zones, so daylight saving changes
 * on their own and the line keeps working without the phone.
 */
object WorldClock {
    /** Shown until the phone app sends the user's own cities. */
    val DEFAULT_CITIES = listOf(
        WorldCity("LDN", ZoneId.of("Europe/London")),
        WorldCity("NYC", ZoneId.of("America/New_York")),
        WorldCity("TOK", ZoneId.of("Asia/Tokyo")),
    )

    /** The arc fits three cities; more would be cut off at the bezel. */
    const val MAX_CITIES = 3

    private const val SEPARATOR = " • "

    fun line(cities: List<WorldCity>, at: Instant, use24Hour: Boolean): String {
        val pattern = if (use24Hour) "HH:mm" else "h:mm"
        val fmt = DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
        return cities.take(MAX_CITIES).joinToString(SEPARATOR) { city ->
            "${city.label} ${fmt.format(at.atZone(city.zone))}"
        }
    }

    /** The short form for a small slot: the first city only, for example "LDN 14:09". */
    fun short(cities: List<WorldCity>, at: Instant, use24Hour: Boolean): String =
        line(cities.take(1), at, use24Hour)
}
