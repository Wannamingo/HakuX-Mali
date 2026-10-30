package com.rfandango.haku_x.data

/** Output/input conveniences; defaults preserve the existing emulator behaviour. */
enum class ExperienceSetting(val key: String, val default: Int, val minimum: Int, val maximum: Int) {
  AUTO_PERFORMANCE("auto_performance", 0, 0, 1),
  VOLUME("volume", 100, 0, 100), MUTE("mute", 0, 0, 1),
  AUDIO_BUFFER("audio_buffer", 1, 0, 2), HRTF("hrtf", 1, 0, 1),
  CROP_LEFT("crop_left", 0, 0, 15), CROP_RIGHT("crop_right", 0, 0, 15),
  CROP_TOP("crop_top", 0, 0, 15), CROP_BOTTOM("crop_bottom", 0, 0, 15),
  ZOOM("zoom", 100, 100, 200), SHIFT_X("shift_x", 0, -20, 20), SHIFT_Y("shift_y", 0, -20, 20),
  REFRESH_RATE("refresh_rate", 0, 0, 240), AUTO_SAVE("auto_save", 0, 0, 1),
  OFFER_RESUME("offer_resume", 0, 0, 1), THERMAL_WARNING("thermal_warning", 0, 0, 1),
  SOC_WARNING("soc_warning", 90, 60, 110), BATTERY_WARNING("battery_warning", 45, 35, 55);

  fun sanitize(value: Int) = value.coerceIn(minimum, maximum)
}

data class ExperienceOptions(val values: Map<ExperienceSetting, Int> = emptyMap()) {
  operator fun get(setting: ExperienceSetting) = setting.sanitize(values[setting] ?: setting.default)
  fun enabled(setting: ExperienceSetting) = get(setting) != 0
  fun audioArgument() = "audio=${get(ExperienceSetting.VOLUME)},${get(ExperienceSetting.MUTE)},${get(ExperienceSetting.AUDIO_BUFFER)},${get(ExperienceSetting.HRTF)}"
  fun framingArgument() = "framing=" + listOf(ExperienceSetting.CROP_LEFT, ExperienceSetting.CROP_RIGHT,
    ExperienceSetting.CROP_TOP, ExperienceSetting.CROP_BOTTOM, ExperienceSetting.ZOOM,
    ExperienceSetting.SHIFT_X, ExperienceSetting.SHIFT_Y).joinToString(",") { get(it).toString() }
}

/** Pure transforms are shared by controller profiles and their tests. */
object StickTransform {
  fun apply(value: Float, deadzone: Float, sensitivity: Float = 1f, invert: Boolean = false): Float {
    if (!value.isFinite()) return 0f
    val dz = deadzone.takeIf { it.isFinite() }?.coerceIn(0f, .95f) ?: 0f
    val gain = sensitivity.takeIf { it.isFinite() }?.coerceIn(.25f, 2f) ?: 1f
    val m = kotlin.math.abs(value)
    val result = if (m <= dz) 0f else kotlin.math.sign(value) * (m - dz) / (1f - dz) * gain
    return (if (invert) -result else result).coerceIn(-1f, 1f)
  }
}

enum class HotkeyAction { MENU, SCREENSHOT, QUICK_SAVE, QUICK_LOAD, SAVE_MENU, LOAD_MENU }

/** Select is deferred until we know whether the press is a shortcut or Xbox Back. */
class HotkeyGate {
  data class Result(val consume: Boolean, val action: HotkeyAction? = null, val flushModifier: Boolean = false)
  private var modifierDown = false
  private var modifierForwarded = false
  private var used = false
  private val consumed = mutableSetOf<Int>()
  private val actions = mutableMapOf<Int, HotkeyAction>()

  fun event(code: Int, down: Boolean, repeat: Boolean, bindings: Map<HotkeyAction, Int>): Result {
    if (!down && consumed.remove(code)) return Result(true, actions.remove(code))
    if (code == MODIFIER) {
      if (down) {
        if (!modifierDown || !repeat) { modifierDown = true; modifierForwarded = false; used = false }
        return Result(!modifierForwarded)
      }
      val flush = modifierDown && !used && !modifierForwarded
      val consume = !modifierForwarded && !flush
      modifierDown = false
      return Result(consume, flushModifier = flush)
    }
    if (modifierDown && down) {
      val action = bindings.entries.firstOrNull { it.value == code && code != 0 }?.key
      if (action != null && !modifierForwarded) {
        consumed += code; used = true
        if (!repeat) actions[code] = action
        return Result(true)
      }
      if (!modifierForwarded && !used) {
        modifierForwarded = true
        return Result(false, flushModifier = true)
      }
    }
    return Result(false)
  }

  fun clear() { modifierDown = false; modifierForwarded = false; used = false; consumed.clear(); actions.clear() }
  companion object { const val MODIFIER = 109 /* KEYCODE_BUTTON_SELECT */ }
}
