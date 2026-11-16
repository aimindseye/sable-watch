package io.github.aimindseye.sablewatch.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CityLabelsTest {
    @Test
    fun `known cities use their usual label`() {
        assertEquals("NYC", CityLabels.label("America/New_York"))
        assertEquals("LDN", CityLabels.label("Europe/London"))
    }

    @Test
    fun `other cities use their first three letters`() {
        assertEquals("BER", CityLabels.label("Europe/Berlin"))
        assertEquals("SAN", CityLabels.label("America/Santiago"))
    }

    @Test
    fun `search finds a city by the start of its name`() {
        val found = CityLabels.search("berl")
        assertEquals("Europe/Berlin", found.first().zone.id)
    }

    @Test
    fun `search ignores blank queries and limits results`() {
        assertTrue(CityLabels.search("  ").isEmpty())
        assertTrue(CityLabels.search("a", limit = 5).size <= 5)
    }

    @Test
    fun `search finds big cities that aren't zone names`() {
        val mumbai = CityLabels.search("mumb").first()
        assertEquals(WorldCity("BOM", java.time.ZoneId.of("Asia/Kolkata")), mumbai)
        assertEquals("Mumbai", CityLabels.cityName(mumbai))
        assertEquals("DEL", CityLabels.search("delhi").first().label)
        assertEquals("BLR", CityLabels.search("bangalore").first().label)
    }

    @Test
    fun `zone cities keep their own name`() {
        assertEquals("Kolkata", CityLabels.cityName(CityLabels.search("kolk").first()))
    }
}
