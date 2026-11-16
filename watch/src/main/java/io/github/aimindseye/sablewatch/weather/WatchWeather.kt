package io.github.aimindseye.sablewatch.weather

import io.github.aimindseye.sablewatch.shared.OpenMeteoApi
import io.github.aimindseye.sablewatch.shared.Weather
import io.github.aimindseye.sablewatch.shared.WeatherSpot
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Open-Meteo straight from the watch, for when the phone has been away for a while. It asks for
 * the city the user picked on the phone (its coordinates came with the last update), never for
 * the watch's location. Needs the watch to have its own Wi-Fi or LTE.
 */
object WatchWeather {
    private const val TIMEOUT_MS = 10_000

    suspend fun fetch(spot: WeatherSpot): Weather? = withContext(Dispatchers.IO) {
        runCatching {
            val url = OpenMeteoApi.forecastUrl(spot.latitude, spot.longitude, spot.fahrenheit)
            val connection = URL(url).openConnection() as HttpsURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                if (connection.responseCode != HttpsURLConnection.HTTP_OK) return@runCatching null
                val current = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                    .optJSONObject("current") ?: return@runCatching null
                if (!current.has("temperature_2m")) return@runCatching null
                Weather(
                    temperature = current.getDouble("temperature_2m").roundToInt(),
                    unit = if (spot.fahrenheit) "F" else "C",
                    condition = OpenMeteoApi.condition(current.optInt("weather_code", -1)),
                    place = spot.name
                )
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
