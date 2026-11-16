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

    /**
     * Big cities that aren't time zone names (India's only zone is Asia/Kolkata, for one), with
     * the zone they use and their usual (airport) label.
     */
    private data class Alias(val name: String, val zone: String, val label: String)

    private val ALIASES = listOf(
        Alias("Mumbai", "Asia/Kolkata", "BOM"),
        Alias("Bombay", "Asia/Kolkata", "BOM"),
        Alias("Delhi", "Asia/Kolkata", "DEL"),
        Alias("New Delhi", "Asia/Kolkata", "DEL"),
        Alias("Bengaluru", "Asia/Kolkata", "BLR"),
        Alias("Bangalore", "Asia/Kolkata", "BLR"),
        Alias("Chennai", "Asia/Kolkata", "MAA"),
        Alias("Hyderabad", "Asia/Kolkata", "HYD"),
        Alias("Pune", "Asia/Kolkata", "PNQ"),
        Alias("Ahmedabad", "Asia/Kolkata", "AMD"),
        Alias("Jaipur", "Asia/Kolkata", "JAI"),
        Alias("Goa", "Asia/Kolkata", "GOI"),
        Alias("Lahore", "Asia/Karachi", "LHE"),
        Alias("Islamabad", "Asia/Karachi", "ISB"),
        Alias("Beijing", "Asia/Shanghai", "BJS"),
        Alias("Shenzhen", "Asia/Shanghai", "SZX"),
        Alias("Guangzhou", "Asia/Shanghai", "CAN"),
        Alias("Osaka", "Asia/Tokyo", "OSA"),
        Alias("Hanoi", "Asia/Bangkok", "HAN"),
        Alias("Abu Dhabi", "Asia/Dubai", "AUH"),
        Alias("Tel Aviv", "Asia/Jerusalem", "TLV"),
        Alias("Frankfurt", "Europe/Berlin", "FRA"),
        Alias("Munich", "Europe/Berlin", "MUC"),
        Alias("Milan", "Europe/Rome", "MIL"),
        Alias("Barcelona", "Europe/Madrid", "BCN"),
        Alias("Geneva", "Europe/Zurich", "GVA"),
        Alias("Manchester", "Europe/London", "MAN"),
        Alias("Edinburgh", "Europe/London", "EDI"),
        Alias("Cape Town", "Africa/Johannesburg", "CPT"),
        Alias("Washington", "America/New_York", "WAS"),
        Alias("Boston", "America/New_York", "BOS"),
        Alias("Miami", "America/New_York", "MIA"),
        Alias("Atlanta", "America/New_York", "ATL"),
        Alias("Philadelphia", "America/New_York", "PHL"),
        Alias("Montreal", "America/Toronto", "YMQ"),
        Alias("Dallas", "America/Chicago", "DFW"),
        Alias("Houston", "America/Chicago", "HOU"),
        Alias("Austin", "America/Chicago", "AUS"),
        Alias("San Francisco", "America/Los_Angeles", "SFO"),
        Alias("Seattle", "America/Los_Angeles", "SEA"),
        Alias("San Diego", "America/Los_Angeles", "SAN"),
        Alias("Las Vegas", "America/Los_Angeles", "LAS"),
        Alias("Rio de Janeiro", "America/Sao_Paulo", "RIO"),
        Alias("Canberra", "Australia/Sydney", "CBR"),
        Alias("Wellington", "Pacific/Auckland", "WLG")
    )

    /** The city part of a zone ID, readable: "America/New_York" becomes "New York". */
    fun cityName(zoneId: String): String = zoneId.substringAfterLast('/').replace('_', ' ')

    /** The name to show for a chosen city: "Mumbai" for BOM, else the zone's own city. */
    fun cityName(city: WorldCity): String =
        ALIASES.firstOrNull { it.label == city.label && it.zone == city.zone.id }?.name ?: cityName(city.zone.id)

    fun label(zoneId: String): String = KNOWN[zoneId]
        ?: cityName(zoneId).filter { it.isLetter() }.take(3).uppercase(Locale.ROOT)

    /** Cities whose name starts with, then contains, [query]; at most [limit]. */
    fun search(query: String, limit: Int = 8): List<WorldCity> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) return emptyList()
        val available = ZoneId.getAvailableZoneIds()
        val aliases = ALIASES.filter { it.zone in available }
            .map { it.name to WorldCity(it.label, ZoneId.of(it.zone)) }
        val names = aliases + available
                .filter { '/' in it && !it.startsWith("Etc/") && !it.startsWith("SystemV/") }
                .sorted()
                .map { cityName(it) to WorldCity(label(it), ZoneId.of(it)) }
        val starts = names.filter { it.first.lowercase(Locale.ROOT).startsWith(q) }
        val contains = names.filter { it !in starts && it.first.lowercase(Locale.ROOT).contains(q) }
        return (starts.sortedBy { it.first } + contains.sortedBy { it.first })
            .map { it.second }
            .distinct()
            .take(limit)
    }
}
