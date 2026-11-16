package io.github.aimindseye.sablewatch.shared

import java.util.Locale

/** The Open-Meteo forecast request and its weather codes, shared by the phone and the watch. */
object OpenMeteoApi {
    private const val FORECAST = "https://api.open-meteo.com/v1/forecast"

    fun forecastUrl(latitude: Double, longitude: Double, fahrenheit: Boolean): String = String.format(
        Locale.ROOT,
        "%s?latitude=%.4f&longitude=%.4f&current=temperature_2m,weather_code&temperature_unit=%s&timezone=auto",
        FORECAST,
        latitude,
        longitude,
        if (fahrenheit) "fahrenheit" else "celsius"
    )

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
}
