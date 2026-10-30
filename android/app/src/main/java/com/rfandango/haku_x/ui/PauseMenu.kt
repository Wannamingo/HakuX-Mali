package com.rfandango.haku_x.ui

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rfandango.haku_x.R
import com.rfandango.haku_x.ui.theme.AppTheme

/** View-based sidebar: fits SDL's Activity and preserves the existing menu pause lifecycle. */
class PauseMenu(private val context: Context, gameTitle: String?, actions: List<Pair<String, () -> Unit>>) {
  private val density = context.resources.displayMetrics.density
  private fun dp(value: Int) = (value * density).toInt()
  private val dark = AppTheme.isDark(context)
  private val menuTextColor = if (dark) Color.rgb(235, 242, 235) else Color.rgb(18, 32, 20)
  private val accent = if (dark) Color.rgb(149, 236, 94) else Color.rgb(35, 111, 44)
  private fun background(fill: Int, outline: Int) = GradientDrawable().apply {
    setColor(fill); cornerRadius = dp(18).toFloat(); setStroke(dp(1), outline)
  }
  val view = ScrollView(context).apply {
    isFillViewport = true
    background = background(if (dark) Color.rgb(15, 23, 17) else Color.rgb(246, 250, 245), accent)
  }
  private val buttons = ArrayList<Button>()
  var onAction: ((() -> Unit) -> Unit)? = null

  init {
    val column = LinearLayout(context).apply {
      orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(22), dp(18), dp(22))
    }
    view.addView(column, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    val header = LinearLayout(context).apply { gravity = Gravity.CENTER_VERTICAL }
    header.addView(ImageView(context).apply { setImageResource(R.mipmap.ic_launcher); importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO }, LinearLayout.LayoutParams(dp(48), dp(48)))
    val titles = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), 0, 0, 0) }
    titles.addView(TextView(context).apply { text = context.getString(R.string.ui_pause_title); textSize = 20f; setTextColor(menuTextColor); setTypeface(typeface, Typeface.BOLD) })
    if (!gameTitle.isNullOrBlank()) titles.addView(TextView(context).apply { text = gameTitle; textSize = 14f; setTextColor(accent); maxLines = 2 })
    header.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    column.addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(18) })
    for ((index, item) in actions.withIndex()) {
      val button = Button(context).apply {
        text = item.first; isAllCaps = false; textSize = 16f; gravity = Gravity.CENTER_VERTICAL or Gravity.START
        minHeight = dp(48); setPadding(dp(16), dp(8), dp(16), dp(8)); setTextColor(menuTextColor)
        background = background(if (dark) Color.rgb(30, 43, 32) else Color.rgb(230, 240, 228), if (index == 0) accent else Color.TRANSPARENT)
        setOnFocusChangeListener { _, focused ->
          background = background(if (focused) (if (dark) Color.rgb(48, 77, 35) else Color.rgb(201, 231, 184))
            else if (dark) Color.rgb(30, 43, 32) else Color.rgb(230, 240, 228), if (focused || index == 0) accent else Color.TRANSPARENT)
        }
        setOnClickListener { onAction?.invoke(item.second) }
      }
      buttons += button
      column.addView(button, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { bottomMargin = dp(8) })
    }
  }

  fun attach(dialog: AlertDialog) {
    dialog.window?.apply {
      setBackgroundDrawableResource(android.R.color.transparent)
      setGravity(Gravity.END or Gravity.CENTER_VERTICAL)
      setLayout(minOf((context.resources.displayMetrics.widthPixels * .7f).toInt(), dp(400)), ViewGroup.LayoutParams.MATCH_PARENT)
      setDimAmount(.55f)
    }
    dialog.setOnKeyListener { _, key, event ->
      when (key) {
        KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_START -> {
          if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) {
            val clicked = dialog.currentFocus?.takeIf { it in buttons }?.performClick() ?: false
            if (!clicked) buttons.firstOrNull()?.performClick()
          }
          true
        }
        KeyEvent.KEYCODE_BUTTON_B -> { if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) dialog.dismiss(); true }
        else -> false
      }
    }
    buttons.firstOrNull()?.requestFocus()
  }
}
