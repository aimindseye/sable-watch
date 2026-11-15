package io.github.aimindseye.sablewatch.phone.data

import android.net.Uri
import androidx.core.net.toUri

/**
 * The content providers the companion can read. Each one decides for itself whether Sable Watch
 * may read it, so every read is allowed to fail; the companion then falls back to the next source.
 */
object Sources {
    /** SableOS Hub: one row per app, columns package and count. Counts only. */
    val SABLE_HUB_APPS: Uri = "content://org.sableos.hub.snapshot/apps".toUri()

    /** Sable Calendar: the next events, columns title, begin, all_day, calendar (and end, when present). */
    val SABLE_CALENDAR_NEXT: Uri = "content://org.sableos.calendar.snapshot/next".toUri()

    /** Sable Weather: one row, columns title, detail ("Place · 21° · Clear"), availability, observed_at. */
    val SABLE_WEATHER_CURRENT: Uri = "content://org.sableos.weather.snapshot/current".toUri()

    /**
     * TenSeven Launcher's Hub: one row per app, columns package and count, behind a signature
     * permission, so only apps signed with the TenSeven key can read it. Counts only.
     */
    val TENSEVEN_HUB_APPS: Uri = "content://io.github.aimindseye.tenseven.hubcounts/apps".toUri()
}
