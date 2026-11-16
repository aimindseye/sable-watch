package io.github.aimindseye.sablewatch.phone.open

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import io.github.aimindseye.sablewatch.phone.MainActivity
import io.github.aimindseye.sablewatch.shared.WatchContract

/**
 * Opens what a tapped bubble on the watch points at (`sablewatch://open/<target>`), then closes:
 * Hub for the message count, the calendar for Agenda Peek, and Sable Watch's own weather or
 * world clock settings. The link carries no data, so any app opening it can only show a screen.
 */
class OpenFromWatchActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val target = intent?.data?.takeIf { it.host == WatchContract.OPEN_HOST }?.lastPathSegment
        when (target) {
            WatchContract.OPEN_HUB -> openHub()
            WatchContract.OPEN_AGENDA -> openCalendar()
            WatchContract.OPEN_CITIES -> openSection(MainActivity.SECTION_CITIES)
            else -> openSection(MainActivity.SECTION_WEATHER)
        }
        finish()
    }

    private fun openHub() {
        if (!tryStart(Intent(ACTION_SHOW_HUB))) openSection(MainActivity.SECTION_MESSAGES)
    }

    private fun openCalendar() {
        val now = CalendarContract.CONTENT_URI.buildUpon()
            .appendPath("time")
            .appendPath(System.currentTimeMillis().toString())
            .build()
        if (!tryStart(Intent(Intent.ACTION_VIEW, now))) openSection(MainActivity.SECTION_AGENDA)
    }

    private fun openSection(section: String) {
        startActivity(MainActivity.intent(this, section))
    }

    private fun Context.tryStart(intent: Intent): Boolean = try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }

    private companion object {
        /** The Hub page of the home screen, in TenSeven Launcher and in SableOS's Start. */
        const val ACTION_SHOW_HUB = "org.sableos.start.action.SHOW_HUB"
    }
}
