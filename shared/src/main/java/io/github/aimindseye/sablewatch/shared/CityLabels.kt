package io.github.aimindseye.sablewatch.shared

import java.time.ZoneId
import java.util.Locale

/** Finds time zones by city name and gives each a three-letter label for the arc. */
object CityLabels {
    /** Labels people expect for big cities, where the first three letters would be odd. */
    private val KNOWN = mapOf(
        "America/New_York" to "NYC",
        "America/Los_Angeles" to "LAX",
        "America/Sao_Paulo" to "SAO",
        "America/Mexico_City" to "MEX",
        "Asia/Kolkata" to "IND",
        "Asia/Hong_Kong" to "HKG",
        "Asia/Tokyo" to "TOK",
        "Asia/Dubai" to "DXB",
        "Australia/Sydney" to "SYD",
        "Europe/London" to "LDN"
    )

    /** The city part of a zone ID, readable: "America/New_York" becomes "New York". */
    fun cityName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')

    fun label(zoneId: String): String = KNOWN[zoneId]
        ?: cityName(zoneId).filter { it.isLetter() }.take(3).uppercase(Locale.ROOT)

    /** Zones whose city name starts with, then contains, [query]; at most [limit]. */
    fun search(query: String, limit: Int = 8): List<WorldCity> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return emptyList()
        val zones = ZoneId.getAvailableZoneIds()
            .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
        val starts = zones.filter { cityName(it).lowercase(Locale.ROOT).startsWith(q) }
        val contains = zones.filter { it !in starts && cityName(it).lowercase(Locale.ROOT).contains(q) }
        return (starts.sorted() + contains.sorted())
            .take(limit)
            .map { WorldCity(label(it), ZoneId.of(it)) }
    }
}
