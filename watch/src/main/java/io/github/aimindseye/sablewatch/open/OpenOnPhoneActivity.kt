package io.github.aimindseye.sablewatch.open

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.google.android.gms.wearable.Wearable
import io.github.aimindseye.sablewatch.R
import io.github.aimindseye.sablewatch.shared.WatchContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await

/**
 * Tapping a Sable bubble lands here: it asks the phone to open the matching screen (Hub for the
 * message count, the calendar for Agenda Peek, Sable Watch's weather settings for the weather),
 * says so for a moment, and closes. Nothing is opened on the watch itself.
 */
class OpenOnPhoneActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val label = TextView(this).apply {
            gravity = Gravity.CENTER
            textSize = TEXT_SP
            setText(R.string.opening_on_phone)
        }
        setContentView(label)
        val target = intent.getStringExtra(EXTRA_TARGET) ?: WatchContract.OPEN_HUB
        scope.launch {
            val opened = runCatching { openOnPhone(target) }.getOrDefault(false)
            label.setText(if (opened) R.string.opened_on_phone else R.string.phone_not_reachable)
            delay(SHOW_MS)
            finish()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private suspend fun openOnPhone(target: String): Boolean {
        val node = Wearable.getNodeClient(this).connectedNodes.await().firstOrNull() ?: return false
        val link = Intent(Intent.ACTION_VIEW, Uri.parse(WatchContract.openUri(target)))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        val future = RemoteActivityHelper(this).startRemoteActivity(link, node.id)
        // Wait for the phone to accept the request, without pulling in a Guava coroutine adapter.
        suspendCancellableCoroutine { done ->
            future.addListener({ done.resumeWith(runCatching { future.get() }) }, mainExecutor)
            done.invokeOnCancellation { future.cancel(true) }
        }
        return true
    }

    companion object {
        private const val EXTRA_TARGET = "target"
        private const val SHOW_MS = 1_500L
        private const val TEXT_SP = 16f

        /** The tap action for a bubble; one request code per target so the extras stay apart. */
        fun tapAction(context: Context, target: String): PendingIntent = PendingIntent.getActivity(
            context,
            target.hashCode(),
            Intent(context, OpenOnPhoneActivity::class.java).putExtra(EXTRA_TARGET, target),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
