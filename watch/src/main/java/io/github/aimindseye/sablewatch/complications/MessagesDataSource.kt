package io.github.aimindseye.sablewatch.complications

import android.graphics.drawable.Icon
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import io.github.aimindseye.sablewatch.R
import io.github.aimindseye.sablewatch.open.OpenOnPhoneActivity
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.sync.StateStore

/**
 * "Sable Hub": the unread count from Hub on the phone (Sable Hub on SableOS, TenSeven's Hub with
 * TenSeven Launcher), or from the phone's message notifications when there is no Hub. A count
 * only: no message text reaches the watch. It is pushed by the phone, so it needs no polling.
 */
class MessagesDataSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, 3)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        StateStore(this).load().unread?.let { build(request.complicationType, it) } ?: NoDataComplicationData()

    private fun build(type: ComplicationType, unread: Int): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val text = if (unread > MAX_SHOWN) "$MAX_SHOWN+" else unread.toString()
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder(text).build(),
            PlainComplicationText.Builder(getString(R.string.messages_description, unread)).build()
        ).setMonochromaticImage(
            MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_hub)).build()
        ).setTapAction(OpenOnPhoneActivity.tapAction(this, WatchContract.OPEN_HUB)).build()
    }

    private companion object {
        const val MAX_SHOWN = 99
    }
}
