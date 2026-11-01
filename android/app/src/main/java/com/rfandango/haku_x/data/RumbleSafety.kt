package com.rfandango.haku_x.data

enum class RumbleMode { AUTO, OFF, ON }

/** GammaOSNext #154: the old GammaPad FF bridge can stall held keys/axes.
 * Match the observed virtual pad and affected build, not every Xbox pad or
 * every GammaOS release. New/unknown builds retain normal rumble in AUTO.
 */
object RumbleSafety {
  fun legacyGammaPad(
    display: String, incremental: String, name: String, descriptor: String,
    vendor: Int, product: Int,
  ): Boolean =
    (incremental.startsWith("eng.gamma.20251222.") || display.contains("eng.gamma.20251222.")) &&
      name == "Xbox Wireless Controller" && vendor == 0x045e && product == 0x02fd &&
      descriptor == "01a85ba7044c3d3c96030d501b88787e6aeb7b9a"

  fun enabled(mode: RumbleMode, legacyGammaPad: Boolean): Boolean = when (mode) {
    RumbleMode.AUTO -> !legacyGammaPad
    RumbleMode.OFF -> false
    RumbleMode.ON -> true
  }
}
