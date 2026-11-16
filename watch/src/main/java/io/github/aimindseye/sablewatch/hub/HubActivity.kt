package io.github.aimindseye.sablewatch.hub

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.text.format.DateFormat
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.aimindseye.sablewatch.R
import io.github.aimindseye.sablewatch.open.OpenOnPhoneActivity
import io.github.aimindseye.sablewatch.shared.HubItem
import io.github.aimindseye.sablewatch.shared.WatchContract
import io.github.aimindseye.sablewatch.sync.PhoneRefresh
import io.github.aimindseye.sablewatch.sync.StateStore
import java.util.Date
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The BB10-style Hub on the watch: the latest conversations, newest first, with the app, who
 * wrote and the start of the message. Tapping one opens it on the phone to read in full and
 * reply. Opening it (or Refresh) asks the phone to sync now. The list shows only when "Hub on
 * the watch" is on in the phone app; otherwise it says how to turn it on.
 */
class HubActivity : Activity() {
    private lateinit var scroll: ScrollView
    private lateinit var list: LinearLayout
    private val store by lazy { StateStore(this) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var status = ""
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        status = ""
        render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val side = (resources.displayMetrics.widthPixels * SIDE_INSET).roundToInt()
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(side, dp(TOP_DP), side, dp(BOTTOM_DP))
        }
        scroll = ScrollView(this).apply {
            setBackgroundColor(Color.BLACK)
            isVerticalScrollBarEnabled = true
            isFocusable = true
            isFocusableInTouchMode = true
            addView(list)
        }
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        render()
        refresh()
        store.listen(listener)
        scroll.requestFocus()
    }

    /** Asks the phone for a sync; the new list arrives through the store and redraws. */
    private fun refresh() {
        status = getString(R.string.hub_refreshing)
        render()
        scope.launch {
            val asked = PhoneRefresh.request(this@HubActivity)
            status = if (asked) "" else getString(R.string.phone_not_reachable)
            render()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onPause() {
        store.stopListening(listener)
        super.onPause()
    }

    /** The crown or bezel scrolls the list. */
    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL && event.isFromSource(InputDevice.SOURCE_ROTARY_ENCODER)) {
            val delta = -event.getAxisValue(MotionEvent.AXIS_SCROLL) *
                ViewConfiguration.get(this).scaledVerticalScrollFactor
            scroll.smoothScrollBy(0, delta.roundToInt())
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    private fun render() {
        list.removeAllViews()
        val items = store.loadHub()
        val unread = items?.sumOf { it.count } ?: 0
        list.addView(text(getString(R.string.hub_title), TITLE_SP, ACCENT, bold = true, center = true))
        val updated = store.load().updatedAtMillis
        val summary = listOfNotNull(
            items?.let { resources.getQuantityString(R.plurals.hub_unread, unread, unread) },
            status.ifEmpty { null } ?: updated.takeIf { it > 0 }?.let {
                DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)
                    .toString()
            }
        ).joinToString(" · ")
        list.addView(text(summary, SMALL_SP, DIM, center = true))
        when {
            items == null -> list.addView(text(getString(R.string.hub_off), BODY_SP, DIM, center = true, top = GAP_DP))

            items.isEmpty() -> list.addView(
                text(getString(R.string.hub_empty), BODY_SP, DIM, center = true, top = GAP_DP)
            )

            else -> items.forEach { list.addView(row(it)) }
        }
        list.addView(button(getString(R.string.hub_refresh), primary = false) { refresh() })
        list.addView(button(getString(R.string.hub_open_on_phone)) { openOnPhone(WatchContract.OPEN_HUB) })
    }

    private fun row(item: HubItem): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(ROW_PAD_DP), dp(ROW_PAD_DP), dp(ROW_PAD_DP), dp(ROW_PAD_DP))
        background = GradientDrawable().apply {
            setColor(ROW)
            cornerRadius = dp(RADIUS_DP).toFloat()
        }
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(ROW_GAP_DP) }
        val header = if (item.count > 1) "${item.app} · ${item.count}" else item.app
        addView(
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(
                    text(header, SMALL_SP, ACCENT),
                    LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                )
                addView(text(timeOf(item.timeMillis), SMALL_SP, DIM))
            }
        )
        if (item.sender.isNotEmpty()) addView(text(item.sender, BODY_SP, Color.WHITE, bold = true))
        if (item.preview.isNotEmpty()) addView(text(item.preview, BODY_SP, LIGHT, maxLines = 2))
        isClickable = true
        contentDescription = listOf(item.app, item.sender, item.preview).joinToString(", ")
        setOnClickListener { openOnPhone("${WatchContract.OPEN_MESSAGE}/${item.id}") }
    }

    private fun button(label: String, primary: Boolean = true, onClick: () -> Unit): View = text(
        label,
        BODY_SP,
        if (primary) Color.BLACK else Color.WHITE,
        bold = true,
        center = true,
        top = GAP_DP
    ).apply {
        setPadding(dp(ROW_PAD_DP), dp(BUTTON_PAD_DP), dp(ROW_PAD_DP), dp(BUTTON_PAD_DP))
        background = GradientDrawable().apply {
            setColor(if (primary) ACCENT else ROW)
            cornerRadius = dp(BUTTON_RADIUS_DP).toFloat()
        }
        isClickable = true
        setOnClickListener { onClick() }
    }

    private fun openOnPhone(target: String) {
        startActivity(OpenOnPhoneActivity.intent(this, target))
    }

    private fun timeOf(millis: Long): String = if (DateUtils.isToday(millis)) {
        DateFormat.getTimeFormat(this).format(Date(millis))
    } else {
        DateUtils.formatDateTime(this, millis, DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH)
    }

    @Suppress("LongParameterList")
    private fun text(
        value: String,
        sp: Float,
        color: Int,
        bold: Boolean = false,
        center: Boolean = false,
        top: Int = 0,
        maxLines: Int = 1
    ): TextView = TextView(this).apply {
        text = value
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        if (center) gravity = Gravity.CENTER_HORIZONTAL
        this.maxLines = maxLines
        ellipsize = TextUtils.TruncateAt.END
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(top) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val ACCENT = 0xFF00A8DF.toInt()
        private const val DIM = 0xFF8C8C8C.toInt()
        private const val LIGHT = 0xFFC8C8C8.toInt()
        private const val ROW = 0xFF161C20.toInt()
        private const val SIDE_INSET = 0.12f
        private const val TOP_DP = 28
        private const val BOTTOM_DP = 48
        private const val GAP_DP = 12
        private const val ROW_GAP_DP = 6
        private const val ROW_PAD_DP = 10
        private const val RADIUS_DP = 12
        private const val BUTTON_PAD_DP = 10
        private const val BUTTON_RADIUS_DP = 22
        private const val TITLE_SP = 18f
        private const val BODY_SP = 14f
        private const val SMALL_SP = 12f

        /** The Hub bubble's tap action. */
        fun tapAction(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, HubActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
