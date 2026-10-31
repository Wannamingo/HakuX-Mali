package com.rfandango.haku_x.data
import com.rfandango.haku_x.R

/** One resolver supplies both startup and live native controls; sentinels stay in the settings store. */
object PostShaders {
  val labels = intArrayOf(R.string.shader_off, R.string.shader_crt_light, R.string.shader_crt_soft,
    R.string.shader_ntsc, R.string.shader_temporal)
  fun label(mode: Int) = labels.getOrElse(mode) { labels[0] }

  val fields = listOf(ExperienceSetting.POST_MASK, ExperienceSetting.POST_MASK_SIZE,
    ExperienceSetting.POST_MASK_STRENGTH, ExperienceSetting.POST_SCANLINES,
    ExperienceSetting.POST_GLOW, ExperienceSetting.POST_BLUR, ExperienceSetting.POST_CURVATURE)
  val allFields = listOf(ExperienceSetting.POST_SHADER) + fields
  val maskLabels = intArrayOf(R.string.shader_mask_none, R.string.shader_mask_stripes,
    R.string.shader_mask_dots, R.string.shader_mask_slots, R.string.shader_mask_diagonal, R.string.shader_mask_sparse)
  fun maskLabel(kind: Int) = maskLabels.getOrElse(kind) { maskLabels[0] }

  fun title(field: ExperienceSetting) = when (field) {
    ExperienceSetting.POST_SHADER -> R.string.shader_title
    ExperienceSetting.POST_MASK -> R.string.shader_mask_type
    ExperienceSetting.POST_MASK_SIZE -> R.string.shader_mask_size
    ExperienceSetting.POST_MASK_STRENGTH -> R.string.shader_mask_strength
    ExperienceSetting.POST_SCANLINES -> R.string.shader_scanline_strength
    ExperienceSetting.POST_GLOW -> R.string.shader_glow
    ExperienceSetting.POST_BLUR -> R.string.shader_blur
    ExperienceSetting.POST_CURVATURE -> R.string.shader_curvature
    else -> error("Not a shader control: $field")
  }

  private fun defaults(mode: Int) = when (mode) {
    2 -> intArrayOf(1, 1, 18, 30, 12, 0, 6)
    4 -> intArrayOf(1, 1, 14, 24, 0, 0, 3)
    else -> intArrayOf(1, 1, 14, 24, 0, 0, 0)
  }

  fun presetValue(field: ExperienceSetting, mode: Int): Int = defaults(mode)[fields.indexOf(field).also { require(it >= 0) }]
  fun followsPreset(options: ExperienceOptions, field: ExperienceSetting) = options[field] == field.default
  fun value(options: ExperienceOptions, field: ExperienceSetting) =
    if (followsPreset(options, field)) presetValue(field, options[ExperienceSetting.POST_SHADER]) else options[field]

  /** JNI: [mode, mask, sizePx, mask%, scanlines%, glow%, blur%, curvature%], no sentinels. */
  fun controls(options: ExperienceOptions): IntArray =
    intArrayOf(options[ExperienceSetting.POST_SHADER], *fields.map { value(options, it) }.toIntArray())
  fun argument(options: ExperienceOptions) = "postshader=" + controls(options).joinToString(",")
  fun calibrationSuffix(options: ExperienceOptions) =
    if (options[ExperienceSetting.POST_SHADER] == 0) "" else "|postshader=v2:" + controls(options).joinToString(",")
}

/** A paused-game dialog changes only a draft until Apply. Null explicitly restores global inheritance. */
class PostShaderDraft(private val base: ExperienceOptions, private val global: ExperienceOptions) {
  private val edits = linkedMapOf<ExperienceSetting, Int?>()
  val changes: Map<ExperienceSetting, Int?> get() = edits.toMap()
  fun options() = ExperienceOptions(base.values + edits.mapValues { (field, value) -> value ?: global[field] })
  fun set(field: ExperienceSetting, value: Int?) {
    require(field in PostShaders.allFields)
    edits[field] = value?.let(field::sanitize)
  }
  fun preset() { PostShaders.fields.forEach { set(it, it.default) } }
  fun inherit() { PostShaders.allFields.forEach { set(it, null) } }
}
