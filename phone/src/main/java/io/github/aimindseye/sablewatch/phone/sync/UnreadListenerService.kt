package io.github.aimindseye.sablewatch.phone.sync

import android.app.Activity
import android.app.ActivityOptions
import android.app.Notification
import android.content.pm.ApplicationInfo
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import io.github.aimindseye.sablewatch.phone.BuildConfig
import io.github.aimindseye.sablewatch.phone.data.Sources
import io.github.aimindseye.sablewatch.shared.HubItem

/**
 * Optional notification access. It tells the companion when to sync (a new notification usually
 * means a new Hub count), and gives a plain message count when no Hub can be read. Only when the
 * user turns on "Hub on the watch" does it also read each message's app, sender and first line,
 * for the watch's Hub list; nothing is kept on the phone.
 */
class UnreadListenerService : NotificationListenerService() {
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = SyncWorker.soon(this@UnreadListenerService)
    }

    override fun onListenerConnected() {
        connected = this
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
        if (connected === this) connected = null
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

    private fun countOf(sbn: StatusBarNotification): Int =
        if (isMessage(sbn)) sbn.notification.number.takeIf { it > 0 } ?: 1 else 0

    private fun isMessage(sbn: StatusBarNotification): Boolean {
        val n = sbn.notification
        val isSummary = n.flags and Notification.FLAG_GROUP_SUMMARY != 0
        val isMessage = n.category in MESSAGE_CATEGORIES || n.extras.containsKey(Notification.EXTRA_MESSAGES)
        return !sbn.isOngoing && !isSummary && isMessage && sbn.packageName != packageName
    }

    /** The message notifications as Hub items; secret (hidden on the lock screen) ones are left out. */
    private fun hubItems(): List<HubItem> = activeNotifications.orEmpty()
        .filter { isMessage(it) && it.notification.visibility != Notification.VISIBILITY_SECRET }
        .map { sbn ->
            val extras = sbn.notification.extras

            @Suppress("DEPRECATION") // The typed overload needs API 33.
            val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            val lastMessage = messages?.lastOrNull()
                ?.let { (it as? Bundle)?.getCharSequence(KEY_MESSAGE_TEXT) }
            HubItem(
                id = idOf(sbn.key),
                app = appName(sbn),
                sender = (
                    extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)
                        ?: extras.getCharSequence(Notification.EXTRA_TITLE)
                    )?.toString().orEmpty(),
                preview = (lastMessage ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty(),
                timeMillis = sbn.notification.`when`.takeIf { it > 0 } ?: sbn.postTime,
                count = countOf(sbn)
            )
        }

    /** The posting app's name: from the notification itself (it carries its ApplicationInfo). */
    private fun appName(sbn: StatusBarNotification): String = runCatching {
        @Suppress("DEPRECATION") // The typed overload needs API 33.
        val info = sbn.notification.extras.getParcelable<ApplicationInfo>(EXTRA_APP_INFO)
            ?: packageManager.getApplicationInfo(sbn.packageName, 0)
        info.loadLabel(packageManager).toString()
    }.getOrNull() ?: sbn.packageName.substringAfterLast('.').replaceFirstChar(Char::uppercaseChar)

    /** Opens the notification behind a Hub item from the watch, as tapping it on the phone would. */
    private fun openItem(id: String, from: Activity): Boolean {
        val sbn = activeNotifications.orEmpty().firstOrNull { idOf(it.key) == id } ?: return false
        val intent = sbn.notification.contentIntent ?: return false
        return runCatching {
            intent.send(from, 0, null, null, null, null, allowStartFromForeground())
            if (sbn.notification.flags and Notification.FLAG_AUTO_CANCEL != 0) cancelNotification(sbn.key)
        }.isSuccess
    }

    companion object {
        private const val KEY_MESSAGE_TEXT = "text"

        /** Notification.EXTRA_BUILDER_APPLICATION_INFO, which every Notification.Builder sets. */
        private const val EXTRA_APP_INFO = "android.appInfo"

        @Volatile
        private var connected: UnreadListenerService? = null

        /** The Hub list for the watch, or null while notification access is off. */
        fun hubItems(): List<HubItem>? = connected?.let { runCatching { it.hubItems() }.getOrNull() }

        /** Opens a Hub item tapped on the watch; false when it is gone or access is off. */
        fun open(id: String, from: Activity): Boolean = connected?.openItem(id, from) ?: false

        /** A short, stable handle for a notification key; the key itself never leaves the phone. */
        private fun idOf(key: String): String = Integer.toHexString(key.hashCode())

        /** Android 14+ needs the app in front to lend its right to start an activity. */
        private fun allowStartFromForeground(): Bundle? = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA -> ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_IF_VISIBLE
                ).toBundle()

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(
                    @Suppress("DEPRECATION")
                    ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                ).toBundle()

            else -> null
        }

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
