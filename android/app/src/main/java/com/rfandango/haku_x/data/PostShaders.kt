package com.rfandango.haku_x.data
import com.rfandango.haku_x.R

/** Fixed prototype presets; values persist independently of geometry and auto profiles. */
object PostShaders {
  val labels = intArrayOf(R.string.shader_off, R.string.shader_crt_light, R.string.shader_crt_soft,
    R.string.shader_ntsc, R.string.shader_temporal)
  fun label(mode: Int) = labels.getOrElse(mode) { labels[0] }
}
