package com.rfandango.haku_x.overlay

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import com.rfandango.haku_x.OnScreenController
import com.rfandango.haku_x.R
import kotlin.math.roundToInt

/** NFSU-style edit workflow; the Xbox control painter and saved layout format stay intact. */
class OverlayEditorBar(context: Context, private val controller: OnScreenController,
  private val onFinished: () -> Unit) : FrameLayout(context) {
  private val density = resources.displayMetrics.density
  private val bar = LinearLayout(context)
  private val panel = ScrollView(context)
  private val tab = action(R.string.overlay_editor_tools) { setFolded(false) }
  private val selection = action(R.string.overlay_editor_all) { controller.selectNextControl() }
  private val resetOne = action(R.string.overlay_editor_reset_one) { controller.resetSelectedAppearance() }
  private val visibility = action(R.string.overlay_editor_hide) { controller.toggleSelectedVisible() }
  private val sizeTitle = label()
  private val opacityTitle = label()
  private val sizeSlider = SeekBar(context)
  private val opacitySlider = SeekBar(context)
  private var refreshing = false
  private var previousSelection: ControlId? = null
  private var rangeInitialized = false
  private var sizeRange = 40..160

  init {
    bar.orientation = LinearLayout.VERTICAL
    bar.setPadding(dp(10), dp(6), dp(10), dp(6))
    panel.background = GradientDrawable().apply {
      cornerRadius = dp(18).toFloat(); setColor(Color.argb(235, 24, 24, 24))
      setStroke(dp(1), Color.argb(70, 255, 255, 255))
    }
    panel.elevation = dp(6).toFloat()
    panel.addView(bar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
    addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.TOP).apply {
      topMargin = dp(8); leftMargin = dp(6); rightMargin = dp(6)
    })
    row().apply {
      addView(selection, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
      addView(action(R.string.overlay_editor_all_short) { controller.selectAllControls() })
      addView(this@OverlayEditorBar.visibility)
      addView(action(R.string.overlay_editor_fold) { setFolded(true) }.apply {
        contentDescription = context.getString(R.string.overlay_editor_fold_desc)
      })
    }
    row().apply {
      addView(sizeTitle, LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
      addView(resetOne)
    }
    slider(sizeSlider) { controller.setScalePercent(sizeRange.first + it) }
    bar.addView(opacityTitle)
    opacitySlider.max = 90
    slider(opacitySlider) { controller.setOpacityPercent(10 + it) }
    row().apply {
      addView(action(R.string.overlay_editor_reset) { controller.resetLayout() }, weighted())
      addView(action(R.string.overlay_editor_cancel) { controller.reloadLayout(); onFinished() }, weighted())
      addView(action(R.string.overlay_editor_save, true) {
        if (controller.saveLayout()) onFinished()
        else Toast.makeText(context, R.string.overlay_editor_save_failed, Toast.LENGTH_LONG).show()
      }, weighted())
    }
    bar.addView(label().apply {
      setText(R.string.overlay_editor_workflow); setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
      setTextColor(0xFFCCCCCC.toInt()); setPadding(0, dp(4), 0, dp(2))
    })
    tab.background = GradientDrawable().apply {
      cornerRadius = dp(12).toFloat(); setColor(Color.argb(225, 24, 24, 24))
    }
    tab.visibility = GONE
    addView(tab, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL))
    controller.onSelectionChanged = { refresh() }
    controller.onLayoutChanged = { refresh() }
    refresh()
  }

  /** Scroll instead of dropping controls on narrow or rotated displays. */
  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val w = MeasureSpec.getSize(widthMeasureSpec).takeIf { it > 0 } ?: dp(440)
    val h = MeasureSpec.getSize(heightMeasureSpec).takeIf { it > 0 } ?: dp(400)
    super.onMeasure(MeasureSpec.makeMeasureSpec(minOf(dp(440), w), MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec((h * .72f).roundToInt(), MeasureSpec.AT_MOST))
  }

  private fun refresh() {
    refreshing = true
    val selected = controller.selectedControl()
    selection.text = context.getString(R.string.overlay_editor_selection,
      selected?.let(::controlTitle) ?: context.getString(R.string.overlay_editor_all))
    val bounds = controller.scaleBounds()
    sizeRange = if (!rangeInitialized || selected != previousSelection) bounds else
      minOf(sizeRange.first, bounds.first)..maxOf(sizeRange.last, bounds.last)
    rangeInitialized = controller.width > 0 && controller.height > 0
    previousSelection = selected
    val percent = controller.scalePercent()
    sizeTitle.text = if (percent == null) context.getString(R.string.overlay_editor_size_mixed)
      else context.getString(R.string.overlay_editor_size_value, percent)
    sizeSlider.max = sizeRange.last - sizeRange.first
    sizeSlider.progress = (percent ?: 100).coerceIn(sizeRange) - sizeRange.first
    val opacity = controller.opacityPercent()
    opacityTitle.text = if (opacity == null) context.getString(R.string.overlay_editor_opacity_mixed)
      else context.getString(R.string.overlay_editor_opacity, opacity)
    opacitySlider.progress = (opacity ?: 50).coerceIn(10, 100) - 10
    resetOne.isEnabled = selected != null
    resetOne.alpha = if (resetOne.isEnabled) 1f else .35f
    visibility.isEnabled = selected != null && selected != ControlId.MENU
    visibility.alpha = if (visibility.isEnabled) 1f else .35f
    visibility.setText(if (controller.isSelectedVisible()) R.string.overlay_editor_hide else R.string.overlay_editor_show)
    refreshing = false
  }

  private fun controlTitle(id: ControlId): String = when (id) {
    ControlId.LEFT_TRIGGER -> context.getString(R.string.overlay_control_lt)
    ControlId.RIGHT_TRIGGER -> context.getString(R.string.overlay_control_rt)
    ControlId.LEFT_STICK -> context.getString(R.string.overlay_control_ls)
    ControlId.RIGHT_STICK -> context.getString(R.string.overlay_control_rs)
    ControlId.LEFT_STICK_CLICK -> "L3"
    ControlId.RIGHT_STICK_CLICK -> "R3"
    ControlId.DPAD -> context.getString(R.string.overlay_control_dpad)
    ControlId.MENU -> context.getString(R.string.overlay_control_menu)
    ControlId.WHITE -> context.getString(R.string.overlay_control_white)
    ControlId.BLACK -> context.getString(R.string.overlay_control_black)
    else -> id.label
  }

  private fun slider(view: SeekBar, change: (Int) -> Unit) {
    view.progressTintList = ColorStateList.valueOf(ACCENT)
    view.thumbTintList = ColorStateList.valueOf(ACCENT)
    view.progressBackgroundTintList = ColorStateList.valueOf(0xFF555555.toInt())
    view.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
      override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && !refreshing) change(progress)
      }
      override fun onStartTrackingTouch(seekBar: SeekBar) = Unit
      override fun onStopTrackingTouch(seekBar: SeekBar) = Unit
    })
    bar.addView(view, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(36)))
  }

  private fun row() = LinearLayout(context).apply {
    orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
    bar.addView(this, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
  }
  private fun weighted() = LinearLayout.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
  private fun label() = TextView(context).apply {
    setTextColor(Color.WHITE); setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    gravity = Gravity.CENTER; maxLines = 2
  }
  private fun action(text: Int, accent: Boolean = false, click: () -> Unit) = label().apply {
    setText(text); setTextColor(if (accent) ACCENT else Color.WHITE); typeface = Typeface.DEFAULT_BOLD
    minHeight = dp(40); minWidth = dp(32); setPadding(dp(6), dp(5), dp(6), dp(5))
    val mask = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(Color.WHITE) }
    background = RippleDrawable(ColorStateList.valueOf(Color.argb(70, 255, 255, 255)), null, mask)
    isClickable = true; isFocusable = true; setOnClickListener { click() }
  }
  fun setFolded(folded: Boolean) {
    panel.visibility = if (folded) GONE else VISIBLE
    tab.visibility = if (folded) VISIBLE else GONE
  }
  private fun dp(value: Int) = (value * density).roundToInt()
  private companion object { val ACCENT = 0xFF8BC34A.toInt() }
}
