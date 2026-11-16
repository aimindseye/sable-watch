package io.github.aimindseye.sablewatch.shared

import java.time.ZoneId

/** The next calendar event for Agenda Peek. [private] events are shown as "Busy" on the watch. */
data class NextEvent(
    val title: String,
    val beginMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val private: Boolean
)

/** The phone's current weather, for the weather bubble. [unit] is "C" or "F". */
data class Weather(val temperature: Int, val unit: String, val condition: String, val place: String)

/**
 * The weather city's coordinates (the city the user picked, never the phone's location), so the
 * watch can fetch Open-Meteo itself when the phone's last update is old.
 */
data class WeatherSpot(val latitude: Double, val longitude: Double, val name: String, val fahrenheit: Boolean)

/** Where the unread count came from, so the phone app can say it. */
enum class UnreadSource { HUB, NOTIFICATIONS }

/**
 * Everything the phone sends the watch, in one Data Layer item at [WatchContract.PATH_STATE].
 * Counts only: no message text, sender or notification content ever leaves the phone.
 */
data class WatchState(
    val cities: List<WorldCity> = WorldClock.DEFAULT_CITIES,
    val nextEvent: NextEvent? = null,
    val unread: Int? = null,
    val unreadSource: UnreadSource? = null,
    val weather: Weather? = null,
    val weatherSpot: WeatherSpot? = null,
    val updatedAtMillis: Long = 0L
)

/**
 * The Data Layer contract. [encode] and [decode] use a plain map so the contract is tested
 * without Google Play services; each app copies the map into or out of a DataMap.
 * Values are only String, Int, Long, Boolean and List<String>.
 */
object WatchContract {
    const val PATH_STATE = "/sablewatch/state"

    /** A message from the watch asking the phone to sync now (the watch's Refresh). */
    const val PATH_REFRESH = "/sablewatch/refresh"

    /** After this long without the phone, the watch fetches weather itself when it can. */
    const val WATCH_WEATHER_AFTER_MS = 60 * 60_000L

    /** After this long without the phone, the Hub bubble shows a "not live" icon. */
    const val STALE_AFTER_MS = 2 * 60 * 60_000L

    /**
     * Links the watch opens on the phone when a bubble is tapped (`sablewatch://open/<target>`).
     * The phone app turns each into the right screen: Hub, the calendar, or its own weather and world clock settings.
     */
    const val OPEN_SCHEME = "sablewatch"
    const val OPEN_HOST = "open"
    const val OPEN_HUB = "hub"
    const val OPEN_AGENDA = "agenda"
    const val OPEN_WEATHER = "weather"
    const val OPEN_CITIES = "cities"

    /** `open/message/<HubItem.id>`: the phone opens that conversation's notification. */
    const val OPEN_MESSAGE = "message"

    fun openUri(target: String): String = "$OPEN_SCHEME://$OPEN_HOST/$target"

    const val KEY_CITIES = "cities"
    const val KEY_EVENT_TITLE = "event_title"
    const val KEY_EVENT_BEGIN = "event_begin"
    const val KEY_EVENT_END = "event_end"
    const val KEY_EVENT_ALL_DAY = "event_all_day"
    const val KEY_EVENT_PRIVATE = "event_private"
    const val KEY_UNREAD = "unread"
    const val KEY_UNREAD_SOURCE = "unread_source"
    const val KEY_WEATHER_TEMP = "weather_temp"
    const val KEY_WEATHER_UNIT = "weather_unit"
    const val KEY_WEATHER_CONDITION = "weather_condition"
    const val KEY_WEATHER_PLACE = "weather_place"
    const val KEY_SPOT_LAT = "spot_lat"
    const val KEY_SPOT_LON = "spot_lon"
    const val KEY_SPOT_NAME = "spot_name"
    const val KEY_SPOT_FAHRENHEIT = "spot_fahrenheit"
    const val KEY_UPDATED_AT = "updated_at"

    private const val CITY_SEPARATOR = '|'

    fun encode(state: WatchState): Map<String, Any> = buildMap {
        put(KEY_CITIES, state.cities.take(WorldClock.MAX_CITIES).map { "${it.label}$CITY_SEPARATOR${it.zone.id}" })
        state.nextEvent?.let {
            put(KEY_EVENT_TITLE, it.title)
            put(KEY_EVENT_BEGIN, it.beginMillis)
            put(KEY_EVENT_END, it.endMillis)
            put(KEY_EVENT_ALL_DAY, it.allDay)
            put(KEY_EVENT_PRIVATE, it.private)
        }
        state.unread?.let { put(KEY_UNREAD, it) }
        state.unreadSource?.let { put(KEY_UNREAD_SOURCE, it.name) }
        state.weather?.let {
            put(KEY_WEATHER_TEMP, it.temperature)
            put(KEY_WEATHER_UNIT, it.unit)
            put(KEY_WEATHER_CONDITION, it.condition)
            put(KEY_WEATHER_PLACE, it.place)
        }
        state.weatherSpot?.let {
            // Strings: the contract carries no floating-point type.
            put(KEY_SPOT_LAT, it.latitude.toString())
            put(KEY_SPOT_LON, it.longitude.toString())
            put(KEY_SPOT_NAME, it.name)
            put(KEY_SPOT_FAHRENHEIT, it.fahrenheit)
        }
        put(KEY_UPDATED_AT, state.updatedAtMillis)
    }

    private fun spot(map: Map<String, Any?>): WeatherSpot? {
        val lat = (map[KEY_SPOT_LAT] as? String)?.toDoubleOrNull() ?: return null
        val lon = (map[KEY_SPOT_LON] as? String)?.toDoubleOrNull() ?: return null
        return WeatherSpot(lat, lon, map[KEY_SPOT_NAME] as? String ?: "", map[KEY_SPOT_FAHRENHEIT] as? Boolean ?: false)
    }

    fun decode(map: Map<String, Any?>): WatchState {
        val cities = (map[KEY_CITIES] as? List<*>)
            ?.mapNotNull { (it as? String)?.let(::parseCity) }
            ?.takeIf { it.isNotEmpty() }
            ?: WorldClock.DEFAULT_CITIES
        val event = (map[KEY_EVENT_TITLE] as? String)?.let { title ->
            val begin = map[KEY_EVENT_BEGIN] as? Long ?: return@let null
            NextEvent(
                title = title,
                beginMillis = begin,
                endMillis = map[KEY_EVENT_END] as? Long ?: begin,
                allDay = map[KEY_EVENT_ALL_DAY] as? Boolean ?: false,
                private = map[KEY_EVENT_PRIVATE] as? Boolean ?: false
            )
        }
        val weather = (map[KEY_WEATHER_TEMP] as? Int)?.let { temp ->
            Weather(
                temperature = temp,
                unit = map[KEY_WEATHER_UNIT] as? String ?: "",
                condition = map[KEY_WEATHER_CONDITION] as? String ?: "",
                place = map[KEY_WEATHER_PLACE] as? String ?: ""
            )
        }
        return WatchState(
            cities = cities,
            nextEvent = event,
            unread = map[KEY_UNREAD] as? Int,
            unreadSource = (map[KEY_UNREAD_SOURCE] as? String)
                ?.let { name -> UnreadSource.entries.firstOrNull { it.name == name } },
            weather = weather,
            weatherSpot = spot(map),
            updatedAtMillis = map[KEY_UPDATED_AT] as? Long ?: 0L
        )
    }

    private fun parseCity(value: String): WorldCity? {
        val label = value.substringBefore(CITY_SEPARATOR, "")
        val zone = value.substringAfter(CITY_SEPARATOR, "")
        if (label.isBlank() || zone.isBlank()) return null
        return runCatching { WorldCity(label, ZoneId.of(zone)) }.getOrNull()
    }
}
