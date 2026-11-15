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
}
