package io.github.aimindseye.sablewatch.phone.sync

import android.app.Notification
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.aimindseye.sablewatch.phone.BuildConfig
import io.github.aimindseye.sablewatch.phone.data.Sources

/**
 * Optional notification access. It does two things: it tells the companion when to sync (a new
 * notification usually means a new Hub count), and it gives a plain message count when no Hub
 * can be read. It only counts: no text, title or sender is read or kept.
 */
class UnreadListenerService : NotificationListenerService() {
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = SyncWorker.soon(this@UnreadListenerService)
    }

    override fun onListenerConnected() {
        recount()
        val watched = buildList {
            add(if (BuildConfig.SABLE_OS) Sources.SABLE_HUB_APPS else Sources.TENSEVEN_HUB_APPS)
            add(CalendarContract.CONTENT_URI)
            if (BuildConfig.SABLE_OS) add(Sources.SABLE_WEATHER_CURRENT)
        }
        watched.forEach { uri -> runCatching { contentResolver.registerContentObserver(uri, true, observer) } }
        SyncWorker.schedulePeriodic(this)
        SyncWorker.soon(this)
    }

    override fun onListenerDisconnected() {
        runCatching { contentResolver.unregisterContentObserver(observer) }
        lastCount = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) = changed()

    override fun onNotificationRemoved(sbn: StatusBarNotification) = changed()

    private fun changed() {
        recount()
        SyncWorker.soon(this)
    }

    private fun recount() {
        lastCount = runCatching { activeNotifications.orEmpty().sumOf(::countOf) }.getOrNull()
    }

    private fun countOf(sbn: StatusBarNotification): Int {
        val n = sbn.notification
        val isSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val isMessage = n.category in MESSAGE_CATEGORIES || n.extras.containsKey(Notification.EXTRA_MESSAGES)
        if (sbn.isOngoing || isSummary || !isMessage || sbn.packageName == packageName) return 0
        return n.number.takeIf { it > 0 } ?: 1
    }

    companion object {
        private val MESSAGE_CATEGORIES = setOf(
            Notification.CATEGORY_MESSAGE,
            Notification.CATEGORY_EMAIL,
            Notification.CATEGORY_SOCIAL,
            Notification.CATEGORY_MISSED_CALL
        )

        /** The last count, or null while notification access is off. */
        @Volatile
        var lastCount: Int? = null
            private set
    }
}
