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
import io.github.aimindseye.sablewatch.weather.WatchWeather
import kotlinx.coroutines.withTimeoutOrNull

/**
 * "Sable Weather": the temperature the phone sent, for example "68°". When the phone has been
 * away for over an hour, the watch asks Open-Meteo itself for the same city (if it is online),
 * at most every half hour; the system calls this every 30 minutes for that.
 */
class WeatherDataSource : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? =
        build(type, Weather(68, "F", "Cloudy", "Jersey City"))

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData =
        current()?.let { build(request.complicationType, it) } ?: NoDataComplicationData()

    private suspend fun current(): Weather? {
        val store = StateStore(this)
        val state = store.load()
        val now = System.currentTimeMillis()
        val spot = state.weatherSpot
        if (spot == null || now - state.updatedAtMillis < WatchContract.WATCH_WEATHER_AFTER_MS) return state.weather
        val cached = store.loadWatchWeather()?.takeIf { (_, at) -> at > state.updatedAtMillis }
        if (cached != null && now - cached.second < REFETCH_MS) return cached.first
        val fresh = withTimeoutOrNull(FETCH_TIMEOUT_MS) { WatchWeather.fetch(spot) }
        if (fresh != null) store.saveWatchWeather(fresh, now)
        return fresh ?: cached?.first ?: state.weather
    }

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

    private companion object {
        const val REFETCH_MS = 30 * 60_000L
        const val FETCH_TIMEOUT_MS = 15_000L
    }
}
