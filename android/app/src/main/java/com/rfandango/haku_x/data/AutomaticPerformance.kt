package com.rfandango.haku_x.data

import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import kotlin.math.roundToInt

/** Conservative FPS-first starting policy, not a measured per-game calibration. */
object AutomaticPerformance {
  fun choose(refreshRates: Iterable<Float>): GraphicsOptions {
    val maximum = refreshRates.filter { it.isFinite() && it >= 24f }.maxOrNull() ?: 60f
    return GraphicsOptions(scale = 1, fxaa = false, upscale = "bilinear", fpsLimit = maximum.roundToInt().coerceIn(24, 120))
  }

  fun forDevice(context: Context): GraphicsOptions {
    @Suppress("DEPRECATION") val display = context.getSystemService(WindowManager::class.java).defaultDisplay
    val powerSave = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    val mode = display.mode
    val rates = if (powerSave) listOf(display.refreshRate) else display.supportedModes.filter {
      it.physicalWidth == mode.physicalWidth && it.physicalHeight == mode.physicalHeight
    }.map { it.refreshRate }
    return choose(rates)
  }
}
