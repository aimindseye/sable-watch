package io.github.aimindseye.sablewatch.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val Accent = Color(0xFF2B9CDB)
internal val RowColor = Color(0xFF141414)
internal val DimColor = Color(0xFF8C8C8C)
private val BarColor = Color(0xFF101010)

@Composable
internal fun Title(text: String) = BasicText(text, style = TextStyle(Color.White, 30.sp))

@Composable
internal fun Dim(text: String) = BasicText(text, style = TextStyle(DimColor, 15.sp))

@Composable
internal fun Body(text: String) =
    BasicText(text, style = TextStyle(Color.White, 16.sp), modifier = Modifier.padding(vertical = 8.dp))

/** A row that Enter or a tap activates; the focused row has the accent outline. */
@Composable
internal fun ActionRow(label: String, detail: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .background(RowColor, shape)
            .border(2.dp, if (focused) Accent else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        BasicText(label, style = TextStyle(Color.White, 18.sp), modifier = Modifier.weight(1f))
        BasicText(detail, style = TextStyle(if (focused) Accent else DimColor, 13.sp))
    }
}

@Composable
internal fun Toggle(label: String, on: Boolean, modifier: Modifier = Modifier, set: (Boolean) -> Unit) =
    ActionRow(label, stringResource(if (on) R.string.on else R.string.off), modifier) { set(!on) }

/** The title, with a tappable back arrow on every screen but the first. */
@Composable
internal fun TopBar(title: String, onBack: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 24.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            BasicText(
                "‹",
                style = TextStyle(Color.White, 34.sp),
                modifier = Modifier
                    .clickable(onClickLabel = stringResource(R.string.back), onClick = onBack)
                    .padding(horizontal = 12.dp)
            )
        } else {
            Spacer(Modifier.width(12.dp))
        }
        Title(title)
    }
}

/** A button that Enter or a tap activates. The primary one is filled with the accent colour. */
@Composable
internal fun PillButton(label: String, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(22.dp)
    BasicText(
        label,
        style = TextStyle(if (primary) Color.Black else Color.White, 16.sp),
        modifier = modifier
            .background(if (primary) Accent else RowColor, shape)
            .border(2.dp, if (focused) Color.White else Color.Transparent, shape)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 11.dp)
    )
}

/** The bar along the bottom: buttons on the left and right, an optional note between them. */
@Composable
internal fun BottomBar(
    modifier: Modifier = Modifier,
    middle: String = "",
    left: @Composable () -> Unit = {},
    right: @Composable () -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth()
            .background(BarColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        left()
        BasicText(
            middle,
            style = TextStyle(DimColor, 13.sp, textAlign = TextAlign.Center),
            modifier = Modifier.weight(1f)
        )
        right()
    }
}
