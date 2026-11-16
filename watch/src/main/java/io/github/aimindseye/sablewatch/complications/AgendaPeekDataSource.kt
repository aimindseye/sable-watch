package io.github.aimindseye.sablewatch.complications

import android.text.format.DateFormat
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import io.github.aimindseye.sablewatch.R
import io.github.aimindseye.sablewatch.open.OpenOnPhoneActivity
import io.github.aimindseye.sablewatch.shared.AgendaPeek
import io.github.aimindseye.sablewatch.shared.NextEvent
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.sync.StateStore
import java.time.Instant
import java.time.ZoneId

/**
 * "Sable Agenda Peek": the next event the phone sent, for the top arc, for example
 * "11:00 AM • DESIGN REVIEW". The event comes from Sable Calendar on SableOS, or from the phone's
 * calendars elsewhere. A timeline switches the line to "NOW" at the start and clears it at the end.
 */
class AgendaPeekDataSource : SuspendingTimelineComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        val begin = Instant.parse("2026-11-09T16:00:00Z").toEpochMilli()
        val sample = NextEvent("Design review", begin, begin + HOUR, allDay = false, private = false)
        return build(type, sample, Instant.ofEpochMilli(begin - HOUR), ZoneId.of("America/New_York"), false)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val type = request.complicationType
        val event = StateStore(this).load().nextEvent
        val now = Instant.now()
        val zone = ZoneId.systemDefault()
        val use24Hour = DateFormat.is24HourFormat(this)
        val first = event?.let { build(type, it, now, zone, use24Hour) } ?: NoDataComplicationData()
        if (event == null) return ComplicationDataTimeline(first, emptyList())
        val changes = AgendaPeek.changes(event, now)
        val entries = changes.mapIndexed { i, from ->
            val until = changes.getOrNull(i + 1) ?: from.plusSeconds(DAY_SECONDS)
            val data = build(type, event, from, zone, use24Hour) ?: NoDataComplicationData()
            TimelineEntry(TimeInterval(from, until), data)
        }
        return ComplicationDataTimeline(first, entries)
    }

    private fun build(
        type: ComplicationType,
        event: NextEvent,
        at: Instant,
        zone: ZoneId,
        use24Hour: Boolean
    ): ComplicationData? {
        val line = AgendaPeek.line(event, at, zone, use24Hour) ?: return null
        val description = PlainComplicationText.Builder(getString(R.string.agenda_label)).build()
        val text = PlainComplicationText.Builder(line).build()
        return when (type) {
            ComplicationType.LONG_TEXT -> LongTextComplicationData.Builder(
                text,
                description
            ).setTapAction(OpenOnPhoneActivity.tapAction(this, WatchContract.OPEN_AGENDA)).build()

            ComplicationType.SHORT_TEXT -> ShortTextComplicationData.Builder(
                PlainComplicationText.Builder(line.substringBefore(" • ")).build(),
                description
            ).setTapAction(OpenOnPhoneActivity.tapAction(this, WatchContract.OPEN_AGENDA)).build()

            else -> null
        }
    }

    private companion object {
        const val HOUR = 3_600_000L
        const val DAY_SECONDS = 86_400L
    }
}
