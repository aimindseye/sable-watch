package io.github.aimindseye.sablewatch.shared

/**
 * One conversation in the watch's Hub list: the app, who wrote, the start of the latest message,
 * and how many are unread. [id] is an opaque handle the phone uses to open the right message.
 */
data class HubItem(
    val id: String,
    val app: String,
    val sender: String,
    val preview: String,
    val timeMillis: Long,
    val count: Int
)

/**
 * The opt-in Hub list ("Hub on the watch", off by default), sent at [PATH] apart from the
 * counts-only state, so turning it off deletes it from the watch. Each item is one string with
 * its fields joined by a separator character that is stripped from the text first.
 */
object HubList {
    const val PATH = "/sablewatch/hub"
    const val KEY_ITEMS = "items"
    const val KEY_UPDATED_AT = "updated_at"

    const val MAX_ITEMS = 20
    const val MAX_PREVIEW = 120
    private const val MAX_NAME = 40
    private const val FIELD = '\u001F'
    private const val FIELDS = 6

    fun encode(items: List<HubItem>): List<String> = items
        .sortedByDescending { it.timeMillis }
        .take(MAX_ITEMS)
        .map {
            listOf(
                clean(it.id, MAX_NAME * 2),
                clean(it.app, MAX_NAME),
                clean(it.sender, MAX_NAME),
                clean(it.preview, MAX_PREVIEW),
                it.timeMillis.toString(),
                it.count.coerceAtLeast(1).toString()
            ).joinToString(FIELD.toString())
        }

    fun decode(encoded: List<String>?): List<HubItem> = encoded.orEmpty().mapNotNull { line ->
        val f = line.split(FIELD)
        if (f.size != FIELDS || f[0].isEmpty()) return@mapNotNull null
        val time = f[4].toLongOrNull() ?: return@mapNotNull null
        HubItem(f[0], f[1], f[2], f[3], time, f[5].toIntOrNull()?.coerceAtLeast(1) ?: 1)
    }

    /** One line of plain text, at most [max] characters, with an ellipsis when cut. */
    fun clean(text: String, max: Int): String {
        val flat = text.replace(FIELD, ' ').replace(Regex("\\s+"), " ").trim()
        return if (flat.length <= max) flat else flat.take(max - 1).trimEnd() + "…"
    }
}
