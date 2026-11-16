package io.github.aimindseye.sablewatch.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.aimindseye.sablewatch.phone.sync.SyncWorker

/** The first-run steps, in order. Each one can be skipped and changed later from the home list. */
private enum class Step(val title: Int) {
    WELCOME(R.string.setup_welcome_title),
    WATCH(R.string.setup_watch_title),
    MESSAGES(R.string.section_messages),
    AGENDA(R.string.section_agenda),
    WEATHER(R.string.section_weather),
    CITIES(R.string.section_cities),
    DONE(R.string.setup_done_title)
}

/**
 * A step-by-step first run: one thing per screen, with Back and Next always on screen.
 * Esc or the system back goes to the previous step; nothing here needs a keyboard.
 */
@Composable
internal fun SetupWizard(status: Status?, changed: () -> Unit, refresh: () -> Unit, finish: () -> Unit) {
    val context = LocalContext.current
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = Step.entries[index]
    val last = Step.entries.size - 1
    val back: () -> Unit = { if (index > 0) index-- }
    val next: () -> Unit = { if (index < last) index++ else finish() }
    BackHandler(enabled = index > 0, onBack = back)
    Column(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Escape && index > 0) {
                    back()
                    true
                } else {
                    false
                }
            }
    ) {
        TopBar(stringResource(step.title), onBack = if (index > 0) back else null)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp)
        ) {
            val first = remember(step) { FocusRequester() }
            val focusFirst = Modifier.focusRequester(first)
            when (step) {
                Step.WELCOME -> {
                    Body(stringResource(R.string.setup_welcome))
                    Dim(stringResource(R.string.platform_line))
                    Spacer(Modifier.height(8.dp))
                    ActionRow(stringResource(R.string.setup_skip), stringResource(R.string.setup_skip_detail)) {
                        finish()
                    }
                }

                Step.WATCH -> {
                    Body(stringResource(R.string.setup_watch))
                    Dim(status?.connection.orEmpty())
                    Spacer(Modifier.height(8.dp))
                    ActionRow(stringResource(R.string.setup_check_again), status?.lastSync.orEmpty(), focusFirst) {
                        SyncWorker.now(context)
                        refresh()
                    }
                    Body(stringResource(R.string.face_help))
                }

                Step.MESSAGES -> MessagesScreen(status, focusFirst, changed)

                Step.AGENDA -> AgendaScreen(status, focusFirst, changed)

                Step.WEATHER -> WeatherScreen(status, focusFirst, changed)

                Step.CITIES -> CitiesScreen(focusFirst, changed)

                Step.DONE -> {
                    Body(stringResource(R.string.setup_done))
                    ActionRow(stringResource(R.string.section_agenda), status?.agenda.orEmpty()) {}
                    ActionRow(stringResource(R.string.section_messages), status?.messages.orEmpty()) {}
                    ActionRow(stringResource(R.string.section_weather), status?.weather.orEmpty()) {}
                    ActionRow(stringResource(R.string.section_cities), status?.cities.orEmpty()) {}
                }
            }
            // The Welcome and Done steps start on the Next button; the others on their first row.
            if (step != Step.WELCOME && step != Step.DONE) {
                LaunchedEffect(step) { runCatching { first.requestFocus() } }
            }
        }
        val nextFocus = remember(step) { FocusRequester() }
        BottomBar(
            middle = stringResource(R.string.setup_progress, index + 1, Step.entries.size),
            left = { if (index > 0) PillButton(stringResource(R.string.back), primary = false, onClick = back) },
            right = {
                PillButton(
                    stringResource(
                        when (step) {
                            Step.WELCOME -> R.string.setup_start
                            Step.DONE -> R.string.setup_finish
                            else -> R.string.next
                        }
                    ),
                    primary = true,
                    modifier = Modifier.focusRequester(nextFocus),
                    onClick = next
                )
            }
        )
        if (step == Step.WELCOME || step == Step.DONE) {
            LaunchedEffect(step) { runCatching { nextFocus.requestFocus() } }
        }
    }
}
