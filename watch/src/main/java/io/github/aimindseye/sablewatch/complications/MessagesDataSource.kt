package io.github.aimindseye.sablewatch.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataTimeline
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingTimelineComplicationDataSourceService
import androidx.wear.watchface.complications.datasource.TimeInterval
import androidx.wear.watchface.complications.datasource.TimelineEntry
import io.github.aimindseye.sablewatch.R
import io.github.aimindseye.sablewatch.hub.HubActivity
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.sync.StateStore
import java.time.Instant

/**
 * "Sable Hub": the unread count from Hub on the phone (Sable Hub on SableOS, TenSeven's Hub with
 * TenSeven Launcher), or from the phone's message notifications when there is no Hub. A count
 * only; the opt-in Hub list behind the tap is separate. It is pushed by the phone, so it needs
 * no polling.
 */
class MessagesDataSource : SuspendingTimelineComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, 3, stale = false)

    /**
     * The count now, and from two hours after the phone's last update the same count with a
     * "not live" icon, so a phone that has gone away shows on the face.
     */
    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationDataTimeline? {
        val state = StateStore(this).load()
        val unread = state.unread ?: return ComplicationDataTimeline(NoDataComplicationData(), emptyList())
        val type = request.complicationType
        val staleAt = Instant.ofEpochMilli(state.updatedAtMillis + WatchContract.STALE_AFTER_MS)
        val stale = build(type, unread, stale = true) ?: NoDataComplicationData()
        if (!Instant.now().isBefore(staleAt)) return ComplicationDataTimeline(stale, emptyList())
        val live = build(type, unread, stale = false) ?: NoDataComplicationData()
        return ComplicationDataTimeline(live, listOf(TimelineEntry(TimeInterval(staleAt, FOREVER), stale)))
    }

    private fun build(type: ComplicationType, unread: Int, stale: Boolean): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val text = if (unread > MAX_SHOWN) "$MAX_SHOWN+" else unread.toString()
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(getString(R.string.messages_description, unread)).build()
        ).setMonochromaticImage(
            MonochromaticImage.Builder(
                Icon.createWithResource(this, if (stale) R.drawable.ic_hub_stale else R.drawable.ic_hub)
            ).build()
        ).setTapAction(HubActivity.tapAction(this)).build()
    }

    private companion object {
        const val MAX_SHOWN = 99
        val FOREVER: Instant = Instant.ofEpochMilli(Long.MAX_VALUE / 2)
    }
}
