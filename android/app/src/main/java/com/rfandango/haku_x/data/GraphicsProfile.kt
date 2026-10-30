package com.rfandango.haku_x.data

/** A preset touches these four settings only; video modes, drivers and game fixes are independent. */
data class GraphicsOptions(val scale: Int, val fxaa: Boolean, val upscale: String, val fpsLimit: Int)

internal fun effectiveFpsLimit(override: String?, global: Int): Int =
  override?.toIntOrNull()?.takeIf { it in 0..240 } ?: global.coerceIn(0, 240)

enum class GraphicsProfile(val options: GraphicsOptions) {
  ECONOMY(GraphicsOptions(1, false, "bilinear", 30)),
  PERFORMANCE(GraphicsOptions(1, false, "bilinear", 60)),
  BALANCED(GraphicsOptions(1, false, "sgsr", 120)),
  QUALITY(GraphicsOptions(2, true, "sgsr", 120));

  companion object {
    fun matching(options: GraphicsOptions): GraphicsProfile? = entries.firstOrNull { it.options == options }
  }
}
