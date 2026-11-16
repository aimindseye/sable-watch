package io.github.aimindseye.sablewatch.phone

import android.Manifest
import android.content.Context
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

/**
 * The companion: a setup wizard on first run, then a home list and its sections. Every screen
 * works by touch (back arrow and buttons) and by keyboard (Esc, Enter, number keys, S).
 */
class MainActivity : ComponentActivity() {
    /** A section to open, from a tapped bubble on the watch; consumed once shown. */
    private var section by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncWorker.schedulePeriodic(this)
        section = intent?.getStringExtra(EXTRA_SECTION)
        setContent { App(section) { section = null } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        section = intent.getStringExtra(EXTRA_SECTION)
    }

    companion object {
        private const val EXTRA_SECTION = "section"
        const val SECTION_MESSAGES = "messages"
        const val SECTION_AGENDA = "agenda"
        const val SECTION_WEATHER = "weather"
        const val SECTION_CITIES = "cities"

        fun intent(context: Context, section: String): Intent = Intent(context, MainActivity::class.java)
            .putExtra(EXTRA_SECTION, section)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

private enum class Screen(val title: Int, val section: String? = null) {
    HOME(R.string.title),
    FACE(R.string.section_face),
    CITIES(R.string.section_cities, MainActivity.SECTION_CITIES),
    AGENDA(R.string.section_agenda, MainActivity.SECTION_AGENDA),
    MESSAGES(R.string.section_messages, MainActivity.SECTION_MESSAGES),
    WEATHER(R.string.section_weather, MainActivity.SECTION_WEATHER),
    HEALTH(R.string.section_health),
    CONNECTION(R.string.section_connection)
}

private val NUMBER_KEYS = listOf(Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven)

@Composable
private fun App(section: String?, sectionShown: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }
    var setupDone by remember { mutableStateOf(settings.setupDone) }
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
    val status = rememberStatus(version)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding()
    ) {
        if (setupDone) {
            MainScreens(status, section, sectionShown, changed, refresh = { version++ }) {
                settings.setupDone = false
                setupDone = false
            }
        } else {
            SetupWizard(status, changed, refresh = { version++ }) {
                settings.setupDone = true
                setupDone = true
                SyncWorker.now(context)
            }
        }
    }
}

/** The home list and its sections, once setup is done. */
@Composable
private fun MainScreens(
    status: Status?,
    section: String?,
    sectionShown: () -> Unit,
    changed: () -> Unit,
    refresh: () -> Unit,
    runSetup: () -> Unit
) {
    val context = LocalContext.current
    var screen by remember { mutableStateOf(Screen.HOME) }
    LaunchedEffect(section) {
        Screen.entries.firstOrNull { section != null && it.section == section }?.let { screen = it }
        if (section != null) sectionShown()
    }
    val home = screen == Screen.HOME
    val back = { screen = Screen.HOME }
    val sync = {
        SyncWorker.now(context)
        refresh()
    }
    BackHandler(enabled = !home, onBack = back)
    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val jump = NUMBER_KEYS.indexOf(event.key)
                when {
                    event.key == Key.Escape && !home -> {
                        back()
                        true
                    }

                    jump >= 0 && home -> {
                        screen = Screen.entries[jump + 1]
                        true
                    }

                    event.key == Key.S && screen !in TYPING_SCREENS -> {
                        sync()
                        true
                    }

                    else -> false
                }
            }
    ) {
        TopBar(stringResource(screen.title), onBack = if (home) null else back)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val first = remember(screen) { FocusRequester() }
            val focusFirst = Modifier.focusRequester(first)
            when (screen) {
                Screen.HOME -> HomeScreen(status, focusFirst, runSetup) { screen = it }
                Screen.FACE -> Body(stringResource(R.string.face_help))
                Screen.CITIES -> CitiesScreen(focusFirst, changed)
                Screen.AGENDA -> AgendaScreen(status, focusFirst, changed)
                Screen.MESSAGES -> MessagesScreen(status, focusFirst)
                Screen.WEATHER -> WeatherScreen(status, focusFirst, changed)
                Screen.HEALTH -> Body(stringResource(R.string.health_help))
                Screen.CONNECTION -> ConnectionScreen(status, focusFirst, refresh)
            }
            LaunchedEffect(screen) { runCatching { first.requestFocus() } }
        }
        BottomBar(
            middle = stringResource(if (home) R.string.hint else R.string.hint_section),
            left = { if (!home) PillButton(stringResource(R.string.back), primary = false, onClick = back) },
            right = { PillButton(stringResource(R.string.sync_now), primary = true, onClick = sync) }
        )
    }
}

@Composable
private fun HomeScreen(status: Status?, modifier: Modifier, runSetup: () -> Unit, open: (Screen) -> Unit) {
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
    Spacer(Modifier.height(8.dp))
    ActionRow(stringResource(R.string.setup_again), "", onClick = runSetup)
}

@Composable
internal fun AgendaScreen(status: Status?, modifier: Modifier, changed: () -> Unit) {
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
internal fun MessagesScreen(status: Status?, modifier: Modifier) {
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
internal fun ConnectionScreen(status: Status?, modifier: Modifier, refresh: () -> Unit) {
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
internal fun rememberStatus(version: Int): Status? {
    val context = LocalContext.current
    val status by produceState<Status?>(null, version) {
        value = withContext(Dispatchers.IO) { Status.read(context) }
    }
    return status
}
