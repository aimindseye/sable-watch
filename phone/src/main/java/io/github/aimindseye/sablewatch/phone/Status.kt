package io.github.aimindseye.sablewatch.phone

import android.content.Context
import android.text.format.DateUtils
import androidx.core.app.NotificationManagerCompat
import io.github.aimindseye.sablewatch.phone.data.PhoneData
import io.github.aimindseye.sablewatch.phone.sync.SyncWorker
import io.github.aimindseye.sablewatch.shared.AgendaPeek
import io.github.aimindseye.sablewatch.shared.UnreadSource
import java.time.Instant
import java.time.ZoneId

/** One-line summaries for the home list and section headers, read fresh each time. */
internal data class Status(
    val connection: String,
    val lastSync: String,
    val cities: String,
    val agenda: String,
    val messages: String,
    val weather: String,
    val calendarAccess: Boolean,
    val listenerOn: Boolean
) {
    companion object {
        fun read(context: Context): Status {
            val settings = Settings(context)
            val data = PhoneData(context)
            val now = System.currentTimeMillis()
            val res = context.resources
            val nodes = settings.lastSyncNodes
            val connection = when {
                settings.lastSyncMillis == 0L -> res.getString(R.string.not_synced)
                nodes == SyncWorker.NO_DATA_LAYER -> res.getString(R.string.no_data_layer)
                nodes == 0 -> res.getString(R.string.not_connected)
                else -> res.getQuantityString(R.plurals.watches_connected, nodes, nodes)
            }
            val lastSync = if (settings.lastSyncMillis == 0L) {
                res.getString(R.string.never_synced)
            } else {
                res.getString(
                    R.string.last_sync,
                    DateUtils.getRelativeTimeSpanString(settings.lastSyncMillis, now, DateUtils.MINUTE_IN_MILLIS)
                )
            }
            val event = data.nextEvent(settings, now)
            val agenda = event
                ?.let {
                    AgendaPeek.line(
                        it,
                        Instant.ofEpochMilli(now),
                        ZoneId.systemDefault(),
                        android.text.format.DateFormat.is24HourFormat(context)
                    )
                }
                ?: res.getString(
                    if (data.hasCalendarAccess() ||
                        BuildConfig.SABLE_OS
                    ) {
                        R.string.no_event
                    } else {
                        R.string.no_calendar
                    }
                )
            val (count, source) = data.unread()
            val messages = when (source) {
                UnreadSource.HUB -> res.getString(R.string.unread_hub, count)
                UnreadSource.NOTIFICATIONS -> res.getString(R.string.unread_notifications, count)
                null -> res.getString(R.string.unread_none)
            }
            val weather = data.weather(settings, now, allowNetwork = false)
                ?.let { res.getString(R.string.weather_line, it.place, it.temperature, it.unit, it.condition) }
                ?: res.getString(R.string.weather_none)
            return Status(
                connection = connection,
                lastSync = lastSync,
                cities = settings.cities.joinToString(" • ") { it.label },
                agenda = agenda,
                messages = messages,
                weather = weather,
                calendarAccess = data.hasCalendarAccess(),
                listenerOn = listenerEnabled(context)
            )
        }

        private fun listenerEnabled(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
    }
}
