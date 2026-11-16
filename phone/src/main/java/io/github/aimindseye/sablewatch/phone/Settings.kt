package io.github.aimindseye.sablewatch.phone

import android.content.Context
import androidx.core.content.edit
import io.github.aimindseye.sablewatch.phone.data.WeatherPlace
import io.github.aimindseye.sablewatch.shared.Weather
import io.github.aimindseye.sablewatch.shared.WorldCity
import io.github.aimindseye.sablewatch.shared.WorldClock
import java.time.ZoneId
import java.util.Locale

/** The companion's own choices, kept on the phone. Nothing here is personal data. */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var cities: List<WorldCity>
        get() = prefs.getString(KEY_CITIES, null)
            ?.split(LIST_SEPARATOR)
            ?.mapNotNull(::parseCity)
            ?.takeIf { it.isNotEmpty() }
            ?: WorldClock.DEFAULT_CITIES
        set(value) = prefs.edit {
            putString(
                KEY_CITIES,
                value.take(WorldClock.MAX_CITIES).joinToString(LIST_SEPARATOR) {
                    "${it.label}$CITY_SEPARATOR${it.zone.id}"
                }
            )
        }

    /** How far ahead Agenda Peek looks, in hours. */
    var windowHours: Int
        get() = prefs.getInt(KEY_WINDOW, WINDOW_CHOICES.first())
        set(value) = prefs.edit { putInt(KEY_WINDOW, value) }

    /** Show every event as "Busy" on the watch. */
    var hideTitles: Boolean
        get() = prefs.getBoolean(KEY_HIDE_TITLES, false)
        set(value) = prefs.edit { putBoolean(KEY_HIDE_TITLES, value) }

    /** Leave all-day events off the arc, so it shows the next timed event. */
    var skipAllDay: Boolean
        get() = prefs.getBoolean(KEY_SKIP_ALL_DAY, true)
        set(value) = prefs.edit { putBoolean(KEY_SKIP_ALL_DAY, value) }

    /** The first-run setup wizard has been finished or skipped. */
    var setupDone: Boolean
        get() = prefs.getBoolean(KEY_SETUP_DONE, false)
        set(value) = prefs.edit { putBoolean(KEY_SETUP_DONE, value) }

    var lastSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SYNC, value) }

    /** The city for Open-Meteo weather, or null to send no weather. */
    var weatherPlace: WeatherPlace?
        get() = prefs.getString(KEY_WEATHER_PLACE, null)?.split(FIELD_SEPARATOR)?.takeIf { it.size == 4 }?.let {
            val lat = it[2].toDoubleOrNull() ?: return null
            val lon = it[3].toDoubleOrNull() ?: return null
            WeatherPlace(it[0], it[1], lat, lon)
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_WEATHER_PLACE)
            } else {
                putString(
                    KEY_WEATHER_PLACE,
                    listOf(value.name, value.detail, value.latitude.toString(), value.longitude.toString())
                        .joinToString(FIELD_SEPARATOR) { f -> f.replace(FIELD_SEPARATOR, " ") }
                )
            }
            remove(KEY_WEATHER_CACHE)
        }

    /** Degrees Fahrenheit for Open-Meteo; Sable Weather keeps its own unit. */
    var fahrenheit: Boolean
        get() = prefs.getBoolean(KEY_FAHRENHEIT, Locale.getDefault().country in FAHRENHEIT_COUNTRIES)
        set(value) = prefs.edit {
            putBoolean(KEY_FAHRENHEIT, value)
            remove(KEY_WEATHER_CACHE)
        }

    /** The last Open-Meteo reading and when it was taken, so the network is used at most every 30 minutes. */
    var weatherCache: Pair<Weather, Long>?
        get() = prefs.getString(KEY_WEATHER_CACHE, null)?.split(FIELD_SEPARATOR)?.takeIf { it.size == 5 }?.let {
            val temp = it[0].toIntOrNull() ?: return null
            val at = it[4].toLongOrNull() ?: return null
            Weather(temp, it[1], it[2], it[3]) to at
        }
        set(value) = prefs.edit {
            if (value == null) {
                remove(KEY_WEATHER_CACHE)
            } else {
                val (w, at) = value
                putString(
                    KEY_WEATHER_CACHE,
                    listOf(w.temperature, w.unit, w.condition, w.place, at).joinToString(FIELD_SEPARATOR)
                )
            }
        }

    /** A hash of what was last sent, so an unchanged state is not sent again. */
    var lastFingerprint: Int
        get() = prefs.getInt(KEY_LAST_FINGERPRINT, 0)
        set(value) = prefs.edit { putInt(KEY_LAST_FINGERPRINT, value) }

    var lastSyncNodes: Int
        get() = prefs.getInt(KEY_LAST_NODES, 0)
        set(value) = prefs.edit { putInt(KEY_LAST_NODES, value) }

    private fun parseCity(value: String): WorldCity? {
        val label = value.substringBefore(CITY_SEPARATOR, "")
        val zone = value.substringAfter(CITY_SEPARATOR, "")
        if (label.isBlank() || zone.isBlank()) return null
        return runCatching { WorldCity(label, ZoneId.of(zone)) }.getOrNull()
    }

    companion object {
        val WINDOW_CHOICES = listOf(4, 12, 24)
        private const val FILE = "sablewatch"
        private const val KEY_CITIES = "cities"
        private const val KEY_WINDOW = "agenda_window_hours"
        private const val KEY_HIDE_TITLES = "agenda_hide_titles"
        private const val KEY_SKIP_ALL_DAY = "agenda_skip_all_day"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_SETUP_DONE = "setup_done"
        private const val KEY_WEATHER_PLACE = "weather_place"
        private const val KEY_WEATHER_CACHE = "weather_cache"
        private const val KEY_FAHRENHEIT = "weather_fahrenheit"
        private const val FIELD_SEPARATOR = "\t"
        private val FAHRENHEIT_COUNTRIES = setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW")
        private const val KEY_LAST_FINGERPRINT = "last_sync_fingerprint"
        private const val KEY_LAST_NODES = "last_sync_nodes"
        private const val LIST_SEPARATOR = ","
        private const val CITY_SEPARATOR = '|'
    }
}
