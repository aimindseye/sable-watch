package io.github.aimindseye.sablewatch.phone.sync

import android.content.Context
import android.net.Uri
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import io.github.aimindseye.sablewatch.shared.HubItem
import io.github.aimindseye.sablewatch.shared.HubList
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

    /** Sends the opt-in Hub list, or deletes it from the watch when [items] is null. */
    suspend fun sendHub(context: Context, items: List<HubItem>?) {
        val client = Wearable.getDataClient(context)
        if (items == null) {
            val uri = Uri.Builder().scheme(PutDataRequest.WEAR_URI_SCHEME).path(HubList.PATH).build()
            client.deleteDataItems(uri, DataClient.FILTER_PREFIX).await()
            return
        }
        val request = PutDataMapRequest.create(HubList.PATH).apply {
            dataMap.putStringArrayList(HubList.KEY_ITEMS, ArrayList(HubList.encode(items)))
        }.asPutDataRequest().setUrgent()
        client.putDataItem(request).await()
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
