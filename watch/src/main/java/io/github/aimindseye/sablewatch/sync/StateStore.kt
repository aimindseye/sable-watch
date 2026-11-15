package io.github.aimindseye.sablewatch.sync

import android.content.Context
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.shared.WatchState
import org.json.JSONArray
import org.json.JSONObject

/**
 * The last state the phone sent, kept on the watch so the complications work while the phone is
 * out of reach. Stored as the contract's map in JSON, in app-private storage.
 */
class StateStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun load(): WatchState {
        val raw = prefs.getString(KEY, null) ?: return WatchState()
        return runCatching { WatchContract.decode(fromJson(JSONObject(raw))) }.getOrDefault(WatchState())
    }

    fun save(state: WatchState) {
        prefs.edit().putString(KEY, toJson(WatchContract.encode(state)).toString()).apply()
    }

    private fun toJson(map: Map<String, Any>): JSONObject = JSONObject().apply {
        map.forEach { (key, value) ->
            when (value) {
                is List<*> -> put(key, JSONArray(value))

                // JSON has one number type; keep the Kotlin type next to Longs so they decode as Long.
                is Long -> put(key, value).put(key + LONG_SUFFIX, true)

                else -> put(key, value)
            }
        }
    }

    private fun fromJson(json: JSONObject): Map<String, Any?> = buildMap {
        json.keys().forEach { key ->
            if (key.endsWith(LONG_SUFFIX)) return@forEach
            val value = json.get(key)
            put(
                key,
                when {
                    value is JSONArray -> (0 until value.length()).map { value.getString(it) }
                    json.optBoolean(key + LONG_SUFFIX) -> json.getLong(key)
                    else -> value
                }
            )
        }
    }

    private companion object {
        const val FILE = "sable_watch_state"
        const val KEY = "state"
        const val LONG_SUFFIX = "#long"
    }
}
