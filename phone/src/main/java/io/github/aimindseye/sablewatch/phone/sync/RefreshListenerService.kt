package io.github.aimindseye.sablewatch.phone.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import io.github.aimindseye.sablewatch.shared.WatchContract

/** The watch's Refresh: syncs now, as Sync now in the app does. The message carries no data. */
class RefreshListenerService : WearableListenerService() {
    override fun onMessageReceived(event: MessageEvent) {
        if (event.path == WatchContract.PATH_REFRESH) SyncWorker.now(this)
    }
}
