package io.github.aimindseye.sablewatch.phone.data

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import io.github.aimindseye.sablewatch.phone.BuildConfig
import io.github.aimindseye.sablewatch.phone.Settings
import io.github.aimindseye.sablewatch.phone.sync.UnreadListenerService
import io.github.aimindseye.sablewatch.shared.NextEvent
import io.github.aimindseye.sablewatch.shared.UnreadSource
import io.github.aimindseye.sablewatch.shared.WatchState
import io.github.aimindseye.sablewatch.shared.Weather
import io.github.aimindseye.sablewatch.shared.WeatherSpot
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Reads what the watch shows from the phone's own apps. Every source may refuse or be missing,
 * so each read falls back to the next and never throws.
 */
class PhoneData(context: Context) {
    private val app = context.applicationContext
    private val resolver: ContentResolver = app.contentResolver

    fun read(settings: Settings, now: Long = System.currentTimeMillis()): WatchState {
        val (unread, source) = unread()
        return WatchState(
            cities = settings.cities,
            nextEvent = nextEvent(settings, now),
            unread = unread,
            unreadSource = source,
            weather = weather(settings, now, allowNetwork = true),
            weatherSpot = settings.weatherPlace?.let {
                WeatherSpot(it.latitude, it.longitude, it.name, settings.fahrenheit)
            },
            updatedAtMillis = now
        )
    }

    /** Where the count comes from, for the companion's Messages screen. */
    fun unread(): Pair<Int?, UnreadSource?> {
        val hub = if (BuildConfig.SABLE_OS) Sources.SABLE_HUB_APPS else Sources.TENSEVEN_HUB_APPS
        Parsing.total(counts(hub))?.let { return it to UnreadSource.HUB }
        return UnreadListenerService.lastCount?.let { it to UnreadSource.NOTIFICATIONS } ?: (null to null)
    }

    fun nextEvent(settings: Settings, now: Long): NextEvent? {
        val rows = (if (BuildConfig.SABLE_OS) sableCalendar() else null)
            ?: systemCalendar(now, settings.windowHours)
            ?: return null
        return Parsing.pickNext(rows, now, settings.windowHours, settings.skipAllDay, settings.hideTitles)
    }

    /**
     * Sable Weather on SableOS; otherwise Open-Meteo for the city the user picked. With
     * [allowNetwork] false (on the main thread), only the last Open-Meteo reading is used.
     */
    fun weather(settings: Settings, now: Long, allowNetwork: Boolean): Weather? {
        if (BuildConfig.SABLE_OS) {
            query(Sources.SABLE_WEATHER_CURRENT) { c ->
                if (c.moveToFirst()) Parsing.weather(c.string("detail"), c.string("availability")) else null
            }?.let { return it }
        }
        val place = settings.weatherPlace ?: return null
        val cached = settings.weatherCache
        if (cached != null && (!allowNetwork || now - cached.second < WEATHER_MAX_AGE_MS)) {
            return cached.first.takeIf { now - cached.second < WEATHER_STALE_MS }
        }
        if (!allowNetwork) return null
        val fresh = OpenMeteo.current(place, settings.fahrenheit)
            ?: return cached?.first?.takeIf { now - cached.second < WEATHER_STALE_MS }
        settings.weatherCache = fresh to now
        return fresh
    }

    fun hasCalendarAccess(): Boolean =
        app.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    private fun counts(uri: Uri): List<Int>? = query(uri) { c ->
        val count = c.getColumnIndex("count")
        if (count < 0) return@query null
        buildList { while (c.moveToNext()) add(c.getInt(count)) }
    }

    private fun sableCalendar(): List<CalendarRow>? = query(Sources.SABLE_CALENDAR_NEXT) { c ->
        buildList {
            while (c.moveToNext()) {
                val begin = c.long("begin") ?: continue
                val allDay = c.int("all_day") == 1
                add(
                    CalendarRow(
                        title = c.string("title").orEmpty(),
                        beginMillis = begin,
                        endMillis = c.long("end") ?: Parsing.defaultEnd(begin, allDay),
                        allDay = allDay,
                        private = false
                    )
                )
            }
        }
    }

    private fun systemCalendar(now: Long, windowHours: Int): List<CalendarRow>? {
        if (!hasCalendarAccess()) return null
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.ACCESS_LEVEL,
            CalendarContract.Instances.SELF_ATTENDEE_STATUS
        )
        // All-day instances are stored at UTC midnight, so look a day back to catch today's.
        val from = now - DAY_MS
        val to = now + windowHours * HOUR_MS
        return runCatching {
            CalendarContract.Instances.query(resolver, projection, from, to)?.use { c ->
                buildList {
                    while (c.moveToNext()) {
                        if (c.getInt(STATUS) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED) continue
                        val allDay = c.getInt(ALL_DAY) == 1
                        add(
                            CalendarRow(
                                title = c.getString(TITLE).orEmpty(),
                                beginMillis = local(c.getLong(BEGIN), allDay),
                                endMillis = local(c.getLong(END), allDay),
                                allDay = allDay,
                                private = c.getInt(ACCESS) == CalendarContract.Events.ACCESS_PRIVATE ||
                                    c.getInt(ACCESS) == CalendarContract.Events.ACCESS_CONFIDENTIAL
                            )
                        )
                    }
                }
            }
        }.getOrNull()
    }

    /** An all-day instance's UTC midnight as the same date's local midnight. */
    private fun local(millis: Long, allDay: Boolean): Long {
        if (!allDay) return millis
        val date = Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC).toLocalDate()
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    /** A provider read that returns null when the provider is missing, refuses, or fails. */
    private fun <T> query(uri: Uri, read: (Cursor) -> T?): T? =
        runCatching { resolver.query(uri, null, null, null, null)?.use(read) }.getOrNull()

    private fun Cursor.string(column: String): String? =
        getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getString)

    private fun Cursor.long(column: String): Long? =
        getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getLong)

    private fun Cursor.int(column: String): Int? =
        getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getInt)

    private companion object {
        const val HOUR_MS = 3_600_000L
        const val DAY_MS = 24 * HOUR_MS
        const val WEATHER_MAX_AGE_MS = 30 * 60_000L
        const val WEATHER_STALE_MS = 3 * HOUR_MS
        const val TITLE = 0
        const val BEGIN = 1
        const val END = 2
        const val ALL_DAY = 3
        const val ACCESS = 4
        const val STATUS = 5
    }
}
