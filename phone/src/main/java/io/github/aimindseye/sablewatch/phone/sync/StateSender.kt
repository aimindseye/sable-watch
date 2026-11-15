package io.github.aimindseye.sablewatch.phone.sync

import android.content.Context
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.shared.WatchState
import kotlinx.coroutines.tasks.await

/** Puts the state in the Data Layer, where the watch app picks it up. */
object StateSender {
    /**
     * Sends [state] and returns how many watches are connected now. Throws when Google Play
     * services has no Wearable API (for example microG), which the caller reports.
     */
    suspend fun send(context: Context, state: WatchState): Int {
        val request = PutDataMapRequest.create(WatchContract.PATH_STATE).apply {
            WatchContract.encode(state).forEach { (key, value) -> dataMap.putValue(key, value) }
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context).putDataItem(request).await()
        return Wearable.getNodeClient(context).connectedNodes.await().size
    }

    private fun DataMap.putValue(key: String, value: Any) {
        when (value) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Long -> putLong(key, value)
            is Boolean -> putBoolean(key, value)
            is List<*> -> putStringArrayList(key, ArrayList(value.filterIsInstance<String>()))
            else -> error("Unsupported value for $key")
        }
    }
}
