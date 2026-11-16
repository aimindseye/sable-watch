package io.github.aimindseye.sablewatch.sync

import android.content.Context
import android.content.SharedPreferences
import io.github.aimindseye.sablewatch.shared.HubItem
import io.github.aimindseye.sablewatch.shared.HubList
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

    /** The Hub list, or null when "Hub on the watch" is off on the phone. */
    fun loadHub(): List<HubItem>? = prefs.getString(KEY_HUB, null)?.let { raw ->
        runCatching { JSONArray(raw) }.getOrNull()?.let { a -> HubList.decode((0 until a.length()).map(a::getString)) }
    }

    fun saveHub(encoded: List<String>?) {
        prefs.edit().apply {
            if (encoded == null) remove(KEY_HUB) else putString(KEY_HUB, JSONArray(encoded).toString())
        }.apply()
    }

    fun listen(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun stopListening(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

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
        const val KEY_HUB = "hub"
        const val LONG_SUFFIX = "#long"
    }
}
