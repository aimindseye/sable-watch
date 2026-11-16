package io.github.aimindseye.sablewatch.complications

import android.text.format.DateFormat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import io.github.aimindseye.sablewatch.R
import io.github.aimindseye.sablewatch.open.OpenOnPhoneActivity
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.shared.WorldClock
import io.github.aimindseye.sablewatch.sync.StateStore
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * "Sable World Clock": up to three cities' times for the top arc of the Sable face. The cities are
 * the ones chosen in the phone app (London, New York and Tokyo until then).
 *
 * A complication can't be refreshed every minute, so each update sends a timeline with one entry
 * per minute for the next hour; Wear OS switches entries on time. The service asks for a new
 * timeline every 30 minutes (UPDATE_PERIOD_SECONDS in the manifest).
 */
class WorldClockDataSource : SuspendingTimelineComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, Instant.parse("2026-11-09T14:09:00Z"), use24Hour = true)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val type = request.complicationType
        val use24Hour = DateFormat.is24HourFormat(this)
        val start = Instant.now().truncatedTo(ChronoUnit.MINUTES)
        val default = build(type, start, use24Hour) ?: return null
        val entries = (1 until TIMELINE_MINUTES).mapNotNull { minute ->
            val from = start.plus(minute.toLong(), ChronoUnit.MINUTES)
            build(type, from, use24Hour)?.let { data ->
                TimelineEntry(TimeInterval(from, from.plus(1, ChronoUnit.MINUTES)), data)
            }
        }
        return ComplicationDataTimeline(default, entries)
    }

    private fun build(type: ComplicationType, at: Instant, use24Hour: Boolean): ComplicationData? {
        val cities = StateStore(this).load().cities
        val description = PlainComplicationText.Builder(getString(R.string.world_clock_label)).build()
        return when (type) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                PlainComplicationText.Builder(WorldClock.line(cities, at, use24Hour)).build(),
                description
            ).setTapAction(OpenOnPhoneActivity.tapAction(this, WatchContract.OPEN_CITIES)).build()

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(WorldClock.short(cities, at, use24Hour)).build(),
                description
            ).setTapAction(OpenOnPhoneActivity.tapAction(this, WatchContract.OPEN_CITIES)).build()

            else -> null
        }
    }

    private companion object {
        const val TIMELINE_MINUTES = 60
    }
}
