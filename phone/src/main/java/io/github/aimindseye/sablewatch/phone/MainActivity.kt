package io.github.aimindseye.sablewatch.phone

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.aimindseye.sablewatch.phone.sync.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The companion: a home list and five sections, keyboard first like the other Sable apps. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedulePeriodic(this)
        setContent { App() }
    }
}

private enum class Screen(val title: Int) {
    HOME(R.string.title),
    FACE(R.string.section_face),
    CITIES(R.string.section_cities),
    AGENDA(R.string.section_agenda),
    MESSAGES(R.string.section_messages),
    WEATHER(R.string.section_weather),
    HEALTH(R.string.section_health),
    CONNECTION(R.string.section_connection)
}

private val NUMBER_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven)

@Composable
private fun App() {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    // Bumped on resume and after every change, so each screen re-reads what it shows.
    var version by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        version++
        onPauseOrDispose { }
    }
    val changed = {
        version++
        SyncWorker.soon(context)
    }
    BackHandler(enabled = screen != Screen.HOME) { screen = Screen.HOME }
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val jump = NUMBER_KEYS.indexOf(event.key)
                when {
                    event.key == Key.Escape && screen != Screen.HOME -> {
                        screen = Screen.HOME
                        true
                    }

                    jump >= 0 && screen == Screen.HOME -> {
                        screen = Screen.entries[jump + 1]
                        true
                    }

                    event.key == Key.S && screen !in TYPING_SCREENS -> {
                        SyncWorker.now(context)
                        true
                    }

                    else -> false
                }
            }
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp)
        ) {
            Title(stringResource(screen.title).lowercase())
            val status = rememberStatus(version)
            val first = remember(screen) { FocusRequester() }
            val focusFirst = Modifier.focusRequester(first)
            when (screen) {
                Screen.HOME -> HomeScreen(status, focusFirst) { screen = it }
                Screen.FACE -> Body(stringResource(R.string.face_help))
                Screen.CITIES -> CitiesScreen(focusFirst, changed)
                Screen.AGENDA -> AgendaScreen(status, focusFirst, changed)
                Screen.MESSAGES -> MessagesScreen(status, focusFirst)
                Screen.WEATHER -> WeatherScreen(status, focusFirst, changed)
                Screen.HEALTH -> Body(stringResource(R.string.health_help))
                Screen.CONNECTION -> ConnectionScreen(status, focusFirst) { version++ }
            }
            LaunchedEffect(screen) { runCatching { first.requestFocus() } }
        }
        HintBar(
            stringResource(if (screen == Screen.HOME) R.string.hint else R.string.hint_section),
            Modifier.align(Alignment.BottomStart)
        )
    }
}

@Composable
private fun HomeScreen(status: Status?, modifier: Modifier, open: (Screen) -> Unit) {
    Dim(status?.connection.orEmpty())
    Dim(stringResource(R.string.platform_line))
    Spacer(Modifier.height(10.dp))
    val details = listOf(
        stringResource(R.string.face_detail),
        status?.cities.orEmpty(),
        status?.agenda.orEmpty(),
        status?.messages.orEmpty(),
        status?.weather.orEmpty(),
        stringResource(R.string.health_detail),
        status?.lastSync.orEmpty()
    )
    Screen.entries.drop(1).forEachIndexed { i, s ->
        ActionRow(
            label = "${i + 1}  ${stringResource(s.title)}",
            detail = details[i],
            modifier = if (i == 0) modifier else Modifier
        ) { open(s) }
    }
}

@Composable
private fun AgendaScreen(status: Status?, modifier: Modifier, changed: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { changed() }
    val access = status?.calendarAccess ?: true
    Dim(status?.agenda.orEmpty())
    Spacer(Modifier.height(8.dp))
    if (!access) {
        ActionRow(
            stringResource(R.string.calendar_allow),
            stringResource(R.string.calendar_allow_detail),
            modifier
        ) {
            ask.launch(Manifest.permission.READ_CALENDAR)
        }
    }
    val window = settings.windowHours
    ActionRow(
        stringResource(R.string.agenda_window),
        pluralStringResource(R.plurals.agenda_window_value, window, window),
        if (access) modifier else Modifier
    ) {
        val choices = Settings.WINDOW_CHOICES
        settings.windowHours = choices[(choices.indexOf(window) + 1) % choices.size]
        changed()
    }
    Toggle(stringResource(R.string.agenda_hide_titles), settings.hideTitles) {
        settings.hideTitles = it
        changed()
    }
    Toggle(stringResource(R.string.agenda_skip_all_day), settings.skipAllDay) {
        settings.skipAllDay = it
        changed()
    }
}

@Composable
private fun MessagesScreen(status: Status?, modifier: Modifier) {
    val context = LocalContext.current
    Dim(status?.messages.orEmpty())
    Spacer(Modifier.height(8.dp))
    Body(stringResource(if (BuildConfig.SABLE_OS) R.string.messages_help_sable else R.string.messages_help_generic))
    ActionRow(
        stringResource(R.string.notification_access),
        stringResource(if (status?.listenerOn == true) R.string.on else R.string.off),
        modifier
    ) {
        context.startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }
}

@Composable
private fun ConnectionScreen(status: Status?, modifier: Modifier, refresh: () -> Unit) {
    val context = LocalContext.current
    Dim(status?.connection.orEmpty())
    Dim(status?.lastSync.orEmpty())
    Spacer(Modifier.height(8.dp))
    ActionRow(stringResource(R.string.sync_now), stringResource(R.string.sync_now_key), modifier) {
        SyncWorker.now(context)
        refresh()
    }
    Body(stringResource(R.string.connection_help))
}

private val TYPING_SCREENS = setOf(Screen.CITIES, Screen.WEATHER)

/** The status lines, read off the main thread; null until the first read finishes. */
@Composable
private fun rememberStatus(version: Int): Status? {
    val context = LocalContext.current
    val status by produceState<Status?>(null, version) {
        value = withContext(Dispatchers.IO) { Status.read(context) }
    }
    return status
}
