package io.github.aimindseye.sablewatch.phone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF2B9CDB)
private val Row = Color(0xFF141414)
private val Dim = Color(0xFF8C8C8C)

/**
 * The companion's home screen (screen 1 of the approved design). The sections are listed but
 * not built yet: they come once pairing with the watch is confirmed on the Q25.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Home() }
    }
}

@Composable
private fun Home() {
    val sections = listOf(
        R.string.section_face,
        R.string.section_cities,
        R.string.section_agenda,
        R.string.section_messages,
        R.string.section_health,
        R.string.section_connection,
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding(),
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
            BasicText(stringResource(R.string.title), style = TextStyle(Color.White, 30.sp))
            BasicText(stringResource(R.string.not_connected), style = TextStyle(Dim, 15.sp))
            BasicText(stringResource(R.string.platform_line), style = TextStyle(Dim, 13.sp))
            Spacer(Modifier.height(10.dp))
            val first = remember { FocusRequester() }
            sections.forEachIndexed { i, label ->
                SectionRow(
                    label = stringResource(label),
                    detail = "${stringResource(R.string.coming_next)} · ${i + 1}",
                    modifier = if (i == 0) Modifier.focusRequester(first) else Modifier,
                )
            }
        }
        BasicText(
            stringResource(R.string.hint),
            style = TextStyle(Dim, 13.sp),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color(0xFF101010))
                .padding(horizontal = 24.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun SectionRow(label: String, detail: String, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(Row, shape)
            .border(2.dp, if (focused) Accent else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        BasicText(label, style = TextStyle(Color.White, 18.sp))
        BasicText(detail, style = TextStyle(Dim, 13.sp))
    }
}
