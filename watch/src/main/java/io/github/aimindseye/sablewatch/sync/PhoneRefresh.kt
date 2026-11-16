package io.github.aimindseye.sablewatch.sync

import android.content.Context
import com.google.android.gms.wearable.Wearable
import io.github.aimindseye.sablewatch.shared.WatchContract
import kotlinx.coroutines.tasks.await

/** Asks the phone to sync now. The answer arrives as a normal state update. */
object PhoneRefresh {
    /** True when at least one connected phone got the request. */
    suspend fun request(context: Context): Boolean = runCatching {
        val nodes = Wearable.getNodeClient(context).connectedNodes.await()
        val messages = Wearable.getMessageClient(context)
        nodes.count { node ->
            runCatching { messages.sendMessage(node.id, WatchContract.PATH_REFRESH, ByteArray(0)).await() }.isSuccess
        } > 0
    }.getOrDefault(false)
}
