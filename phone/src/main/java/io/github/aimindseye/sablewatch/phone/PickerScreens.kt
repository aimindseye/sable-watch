package io.github.aimindseye.sablewatch.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aimindseye.sablewatch.phone.data.OpenMeteo
import io.github.aimindseye.sablewatch.phone.data.WeatherPlace
import io.github.aimindseye.sablewatch.shared.CityLabels
import io.github.aimindseye.sablewatch.shared.WorldClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val SEARCH_DEBOUNCE_MS = 400L

/** World clock cities: the chosen three (Enter removes one) and a search to add more. */
@Composable
internal fun CitiesScreen(modifier: Modifier, changed: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var cities by remember { mutableStateOf(settings.cities) }
    var query by remember { mutableStateOf("") }
    Dim(stringResource(R.string.cities_help, WorldClock.MAX_CITIES))
    SearchField(query, stringResource(R.string.cities_search), modifier) { query = it }
    if (query.isNotBlank()) {
        CityLabels.search(query).forEach { city ->
            ActionRow(CityLabels.cityName(city), city.label) {
                cities = (cities - city + city).takeLast(WorldClock.MAX_CITIES)
                settings.cities = cities
                query = ""
                changed()
            }
        }
    }
    cities.forEach { city ->
        ActionRow("${city.label}  ${CityLabels.cityName(city)}", stringResource(R.string.remove)) {
            cities = (cities - city).ifEmpty { WorldClock.DEFAULT_CITIES }
            settings.cities = cities
            changed()
        }
    }
}

/** The Open-Meteo city, and the unit. On SableOS, Sable Weather is used first when it answers. */
@Composable
internal fun WeatherScreen(status: Status?, modifier: Modifier, changed: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var place by remember { mutableStateOf(settings.weatherPlace) }
    var fahrenheit by remember { mutableStateOf(settings.fahrenheit) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<WeatherPlace>()) }
    LaunchedEffect(query) {
        results = emptyList()
        if (query.length < 2) return@LaunchedEffect
        delay(SEARCH_DEBOUNCE_MS)
        results = withContext(Dispatchers.IO) { OpenMeteo.search(query) }
    }
    Dim(status?.weather.orEmpty())
    Body(stringResource(if (BuildConfig.SABLE_OS) R.string.weather_help_sable else R.string.weather_help_generic))
    SearchField(query, stringResource(R.string.weather_search), modifier) { query = it }
    results.forEach { result ->
        ActionRow(result.name, result.detail) {
            settings.weatherPlace = result
            place = result
            query = ""
            changed()
        }
    }
    place?.let {
        ActionRow(it.name, stringResource(R.string.remove)) {
            settings.weatherPlace = null
            place = null
            changed()
        }
    }
    ActionRow(stringResource(R.string.weather_unit), if (fahrenheit) "°F" else "°C") {
        fahrenheit = !fahrenheit
        settings.fahrenheit = fahrenheit
        changed()
    }
}

@Composable
private fun SearchField(value: String, hint: String, modifier: Modifier, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = TextStyle(Color.White, 18.sp),
        cursorBrush = SolidColor(Accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions.Default,
        modifier = modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .background(RowColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 11.dp),
        decorationBox = { inner ->
            if (value.isEmpty()) Dim(hint)
            inner()
        }
    )
}
