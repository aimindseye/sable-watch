package io.github.aimindseye.sablewatch.phone.data

import io.github.aimindseye.sablewatch.shared.Weather
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt
import org.json.JSONObject

/** A place the user picked for weather. Only its name and coordinates are kept. */
data class WeatherPlace(val name: String, val detail: String, val latitude: Double, val longitude: Double)

/**
 * Weather from Open-Meteo (open-meteo.com), for phones without Sable Weather. No key or account;
 * the request carries only the chosen city's coordinates, never the phone's location.
 */
object OpenMeteo {
    private const val GEOCODING = "https://geocoding-api.open-meteo.com/v1/search"
    private const val FORECAST = "https://api.open-meteo.com/v1/forecast"
    private const val TIMEOUT_MS = 10_000
    private const val MAX_RESULTS = 8

    fun searchUrl(query: String, language: String): String =
        "$GEOCODING?name=${URLEncoder.encode(query.trim(), "UTF-8")}&count=$MAX_RESULTS&language=$language&format=json"

    fun forecastUrl(place: WeatherPlace, fahrenheit: Boolean): String = String.format(
        Locale.ROOT,
        "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&temperature_unit=%s&timezone=auto",
        FORECAST,
        place.latitude,
        place.longitude,
        if (fahrenheit) "fahrenheit" else "celsius"
    )

    /** Cities matching [query]; empty when there are none or the request failed. */
    fun search(query: String, language: String = Locale.getDefault().language): List<WeatherPlace> {
        if (query.isBlank()) return emptyList()
        val json = get(searchUrl(query, language)) ?: return emptyList()
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).map { i ->
            val r = results.getJSONObject(i)
            WeatherPlace(
                name = r.optString("name"),
                detail = listOf(
                    r.optString("admin1"),
                    r.optString("country")
                ).filter(String::isNotBlank).joinToString(", "),
                latitude = r.getDouble("latitude"),
                longitude = r.getDouble("longitude")
            )
        }
    }

    /** The current temperature and condition at [place]; null when the request failed. */
    fun current(place: WeatherPlace, fahrenheit: Boolean): Weather? {
        val current = get(forecastUrl(place, fahrenheit))?.optJSONObject("current") ?: return null
        if (!current.has("temperature_2m")) return null
        return Weather(
            temperature = current.getDouble("temperature_2m").roundToInt(),
            unit = if (fahrenheit) "F" else "C",
            condition = condition(current.optInt("weather_code", -1)),
            place = place.name
        )
    }

    /** A short label for a WMO weather code, as Open-Meteo reports it. */
    @Suppress("MagicNumber") // The WMO code table.
    fun condition(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2 -> "Partly cloudy"
        3 -> "Cloudy"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67, in 80..82 -> "Rain"
        in 71..77, 85, 86 -> "Snow"
        in 95..99 -> "Storm"
        else -> ""
    }

    private fun get(url: String): JSONObject? = runCatching {
        val connection = URL(url).openConnection() as HttpsURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpsURLConnection.HTTP_OK) return@runCatching null
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }.getOrNull()
}
