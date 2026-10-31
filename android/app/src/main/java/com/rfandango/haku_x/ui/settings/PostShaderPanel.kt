package com.rfandango.haku_x.ui.settings

import android.content.Context
import android.view.View
import android.widget.*
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.*

/** Platform widgets work inside SDLActivity without a Compose lifecycle owner. */
class PostShaderPanel(private val context: Context, base: ExperienceOptions, global: ExperienceOptions) {
  private val draft = PostShaderDraft(base, global)
  private val body = LinearLayout(context).apply {
    orientation = LinearLayout.VERTICAL
    setPadding(dp(20), dp(8), dp(20), dp(8))
  }
  val view = ScrollView(context).apply { addView(body) }
  val changes get() = draft.changes
  private fun dp(value: Int) = (value * context.resources.displayMetrics.density).toInt()
  init { refresh() }

  private fun text(value: String) = TextView(context).apply {
    text = value; textSize = 16f; setPadding(0, dp(8), 0, dp(4))
  }
  private fun refresh() {
    val scroll = view.scrollY
    body.removeAllViews()
    body.addView(text(context.getString(R.string.shader_controls_desc)))
    choice(ExperienceSetting.POST_SHADER, (0..4).toList())
    choice(ExperienceSetting.POST_MASK, (-1..5).toList())
    choice(ExperienceSetting.POST_MASK_SIZE, (0..8).toList())
    for (field in PostShaders.fields.drop(2)) slider(field)
    body.addView(Button(context).apply {
      setText(R.string.shader_reset_parameters)
      setOnClickListener { draft.preset(); refresh() }
    })
    body.addView(Button(context).apply {
      setText(R.string.shader_use_global)
      setOnClickListener { draft.inherit(); refresh() }
    })
    view.post { view.scrollTo(0, scroll) }
  }
  private fun label(field: ExperienceSetting, value: Int): String = when {
    field == ExperienceSetting.POST_SHADER -> context.getString(PostShaders.label(value))
    value == field.default -> context.getString(R.string.shader_from_preset)
    field == ExperienceSetting.POST_MASK -> context.getString(PostShaders.maskLabel(value))
    else -> context.getString(R.string.shader_pixels, value)
  }
  private fun choice(field: ExperienceSetting, values: List<Int>) {
    val current = draft.options()[field]
    body.addView(text(context.getString(PostShaders.title(field))))
    body.addView(Spinner(context).apply {
      contentDescription = context.getString(PostShaders.title(field))
      adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, values.map { label(field, it) })
      setSelection(values.indexOf(current).coerceAtLeast(0), false)
      minimumHeight = dp(48)
      onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
          if (values[position] != draft.options()[field]) { draft.set(field, values[position]); refresh() }
        }
      }
    })
  }
  private fun slider(field: ExperienceSetting) {
    fun description(): String {
      val options = draft.options()
      val prefix = if (PostShaders.followsPreset(options, field)) context.getString(R.string.shader_from_preset) + " · " else ""
      return context.getString(PostShaders.title(field)) + ": " + prefix +
        context.getString(R.string.shader_percent, PostShaders.value(options, field))
    }
    val title = text(description())
    body.addView(title)
    body.addView(SeekBar(context).apply {
      contentDescription = context.getString(PostShaders.title(field))
      max = field.maximum; progress = PostShaders.value(draft.options(), field)
      minimumHeight = dp(48)
      setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
          if (fromUser) { draft.set(field, progress); title.text = description() }
        }
      })
    })
  }
}
