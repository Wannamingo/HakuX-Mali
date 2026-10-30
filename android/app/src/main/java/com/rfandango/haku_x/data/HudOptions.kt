package com.rfandango.haku_x.data

enum class HudPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

data class HudOptions(
  val enabled: Boolean = true,
  val textSize: Int = 11,
  val position: HudPosition = HudPosition.TOP_LEFT,
  val background: Int = 40,
  val cpu: Boolean = true,
  val gpu: Boolean = true,
  val battery: Boolean = true,
) {
  val temperatures get() = cpu || gpu || battery
  fun sanitized() = copy(textSize = textSize.coerceIn(8, 18), background = background.coerceIn(0, 100))
}
