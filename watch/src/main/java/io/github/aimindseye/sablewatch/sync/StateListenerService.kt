package io.github.aimindseye.sablewatch.sync

import android.content.ComponentName
import android.content.Context
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import io.github.aimindseye.sablewatch.complications.AgendaPeekDataSource
import io.github.aimindseye.sablewatch.complications.MessagesDataSource
import io.github.aimindseye.sablewatch.complications.WeatherDataSource
import io.github.aimindseye.sablewatch.complications.WorldClockDataSource
import io.github.aimindseye.sablewatch.shared.HubList
import io.github.aimindseye.sablewatch.shared.WatchContract

/** Receives the phone's state over the Data Layer, stores it, and refreshes the Sable complications. */
class StateListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        // The opt-in Hub list: stored when sent, forgotten when the phone deletes it.
        events.lastOrNull { it.dataItem.uri.path == HubList.PATH }?.let { event ->
            val items = if (event.type == DataEvent.TYPE_CHANGED) {
                DataMapItem.fromDataItem(event.dataItem).dataMap.getStringArrayList(HubList.KEY_ITEMS).orEmpty()
            } else {
                null
            }
            StateStore(this).saveHub(items)
        }
        val latest = events
            .filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == WatchContract.PATH_STATE }
            .lastOrNull()
            ?.let { DataMapItem.fromDataItem(it.dataItem).dataMap }
            ?: return
        StateStore(this).save(WatchContract.decode(latest.toMap()))
        refreshComplications(this)
    }

    companion object {
        fun refreshComplications(context: Context) {
            listOf(
                AgendaPeekDataSource::class.java,
                WorldClockDataSource::class.java,
                MessagesDataSource::class.java,
                WeatherDataSource::class.java
            ).forEach { source ->
                ComplicationDataSourceUpdateRequester
                    .create(context, ComponentName(context, source))
                    .requestUpdateAll()
            }
        }
    }
}

/** The DataMap as the contract's plain map. */
internal fun DataMap.toMap(): Map<String, Any?> = keySet().associateWith { key ->
    @Suppress("DEPRECATION")
    val value = get<Any?>(key)
    if (value is ArrayList<*>) value.toList() else value
}
