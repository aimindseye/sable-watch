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
import io.github.aimindseye.sablewatch.shared.Weather
import io.github.aimindseye.sablewatch.sync.StateStore

/** "Sable Weather": the temperature from Sable Weather on the phone, for example "68°". */
class WeatherDataSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, Weather(68, "F", "Cloudy", "Jersey City"))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        StateStore(this).load().weather?.let { build(request.complicationType, it) } ?: NoDataComplicationData()

    private fun build(type: ComplicationType, weather: Weather): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) return null
        val description = listOf(weather.place, weather.condition).filter { it.isNotBlank() }.joinToString(", ")
        return ShortTextComplicationData.Builder(
            PlainComplicationText.Builder("${weather.temperature}°").build(),
            PlainComplicationText.Builder(description.ifBlank { getString(R.string.weather_label) }).build()
        ).setMonochromaticImage(
            MonochromaticImage.Builder(Icon.createWithResource(this, R.drawable.ic_weather)).build()
        ).setTapAction(OpenOnPhoneActivity.tapAction(this, WatchContract.OPEN_WEATHER)).build()
    }
}
