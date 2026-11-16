package io.github.aimindseye.sablewatch.phone.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import io.github.aimindseye.sablewatch.phone.Settings
import io.github.aimindseye.sablewatch.phone.data.PhoneData
import io.github.aimindseye.sablewatch.shared.WatchContract
import java.util.concurrent.TimeUnit

/**
 * Reads the phone's sources and sends them to the watch. Runs every 15 minutes, soon after a
 * notification or calendar change, and when the next event starts or ends.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = Settings(applicationContext)
        val now = System.currentTimeMillis()
        val state = PhoneData(applicationContext).read(settings, now)
        val fingerprint = WatchContract.encode(state.copy(updatedAtMillis = 0L)).toSortedMap().toString().hashCode()
        val force = inputData.getBoolean(KEY_FORCE, false)
        // Unchanged state is still re-sent every half hour, so the watch can tell "nothing new"
        // from "the phone is gone" (it fetches weather itself and marks Hub as not live).
        val due = now - settings.lastSentMillis >= RESEND_MS
        if (force || due || fingerprint != settings.lastFingerprint) {
            val nodes = runCatching { StateSender.send(applicationContext, state) }
                .onFailure { Log.w(TAG, "Could not reach the Wear OS Data Layer", it) }
                .getOrElse {
                    settings.lastSyncNodes = NO_DATA_LAYER
                    return if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
                }
            settings.lastFingerprint = fingerprint
            settings.lastSentMillis = now
            settings.lastSyncNodes = nodes
        }
        // The opt-in Hub list. The Data Layer drops a put that changes nothing, so it is sent
        // every time; with the setting off it is deleted (a no-op when it was never sent).
        val hub = if (settings.hubOnWatch) UnreadListenerService.hubItems().orEmpty() else null
        runCatching { StateSender.sendHub(applicationContext, hub) }
            .onFailure { Log.w(TAG, "Could not send the Hub list", it) }
        settings.lastSyncMillis = now
        state.nextEvent?.let { event ->
            // The watch moves the arc on its own at start and end; after the end it needs the next event.
            val delay = event.endMillis - now + BOUNDARY_SLACK_MS
            if (delay in 1..MAX_BOUNDARY_MS) enqueue(applicationContext, NAME_BOUNDARY, delay, force = false)
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "SableWatchSync"
        private const val NAME_PERIODIC = "sync-periodic"
        private const val NAME_SOON = "sync-soon"
        private const val NAME_BOUNDARY = "sync-boundary"
        const val NAME_NOW = "sync-now"

        /** Stored as the node count when the Data Layer could not be reached at all. */
        const val NO_DATA_LAYER = -1
        private const val MAX_ATTEMPTS = 3
        private const val KEY_FORCE = "force"
        private const val SOON_DELAY_MS = 3_000L
        private const val BOUNDARY_SLACK_MS = 5_000L
        private const val MAX_BOUNDARY_MS = 24 * 3_600_000L
        private const val PERIOD_MINUTES = 15L
        private const val RESEND_MS = 30 * 60_000L

        fun schedulePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<SyncWorker>(PERIOD_MINUTES, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(NAME_PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request)
        }

        /** After a change: waits a moment so a burst of notifications sends once. */
        fun soon(context: Context) = enqueue(context, NAME_SOON, SOON_DELAY_MS, force = false)

        /** The Sync now button: sends even when nothing changed. */
        fun now(context: Context) = enqueue(context, NAME_NOW, 0L, force = true)

        private fun enqueue(context: Context, name: String, delayMs: Long, force: Boolean) {
            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(KEY_FORCE to force))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(name, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
