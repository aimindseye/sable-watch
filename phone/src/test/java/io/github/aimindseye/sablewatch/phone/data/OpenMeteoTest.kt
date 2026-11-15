package io.github.aimindseye.sablewatch.phone.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenMeteoTest {
    @Test
    fun forecastSendsOnlyTheCityCoordinates() {
        val url = OpenMeteo.forecastUrl(
            WeatherPlace("Toronto", "Ontario, Canada", 43.70011, -79.4163),
            fahrenheit = false
        )
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue(url.contains("latitude=43.7001&longitude=-79.4163"))
        assertTrue(url.contains("temperature_unit=celsius"))
    }

    @Test
    fun searchEncodesTheQuery() {
        val url = OpenMeteo.searchUrl(" São Paulo ", "pt")
        assertTrue(url.startsWith("https://geocoding-api.open-meteo.com/v1/search?name=S%C3%A3o+Paulo&"))
        assertTrue(url.contains("language=pt"))
    }

    @Test
    fun conditions() {
        assertEquals("Clear", OpenMeteo.condition(0))
        assertEquals("Rain", OpenMeteo.condition(63))
        assertEquals("Rain", OpenMeteo.condition(81))
        assertEquals("Snow", OpenMeteo.condition(75))
        assertEquals("Storm", OpenMeteo.condition(95))
        assertEquals("", OpenMeteo.condition(-1))
    }
}
