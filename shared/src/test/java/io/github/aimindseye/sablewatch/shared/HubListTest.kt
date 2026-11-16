package io.github.aimindseye.sablewatch.shared

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HubListTest {
    private fun item(id: String, time: Long, preview: String = "See you at 6") =
        HubItem(id, "Signal", "Dana", preview, time, 2)

    @Test
    fun `round trip keeps every field, newest first`() {
        val items = listOf(item("a", 1_000L), item("b", 3_000L), item("c", 2_000L))
        val back = HubList.decode(HubList.encode(items))
        assertEquals(listOf("b", "c", "a"), back.map { it.id })
        assertEquals(item("b", 3_000L), back.first())
    }

    @Test
    fun `keeps at most the newest twenty`() {
        val items = (1..30).map { item("$it", it.toLong()) }
        val back = HubList.decode(HubList.encode(items))
        assertEquals(HubList.MAX_ITEMS, back.size)
        assertEquals("30", back.first().id)
    }

    @Test
    fun `text is one line, cut with an ellipsis, and can't break the fields`() {
        val long = "line one\nline two\u001Fsneaky " + "x".repeat(200)
        val back = HubList.decode(HubList.encode(listOf(item("a", 1L, long)))).single()
        assertEquals(HubList.MAX_PREVIEW, back.preview.length)
        assertTrue(back.preview.startsWith("line one line two sneaky"))
        assertTrue(back.preview.endsWith("…"))
    }

    @Test
    fun `malformed entries are skipped`() {
        assertEquals(emptyList<HubItem>(), HubList.decode(listOf("nope", "")))
        assertEquals(emptyList<HubItem>(), HubList.decode(null))
    }
}
