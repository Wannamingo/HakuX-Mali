package com.rfandango.haku_x.data

import java.util.Locale

/** Virtual-clock speed and an explicitly configured frame-rate estimate are different metrics. */
object EmulationSpeed {
  val referenceRates = listOf(0, 25, 30, 50, 60, 120)

  data class Reading(val clockPercent: Float?, val framePercent: Float?, val referenceFps: Int, val paused: Boolean)

  fun read(stats: FloatArray, referenceFps: Int): Reading {
    val reference = referenceFps.takeIf { it in referenceRates } ?: 0
    val status = stats.getOrNull(5)?.takeIf { it.isFinite() }?.toInt()
    val paused = status == 3
    val clock = if (status == 2) stats.getOrNull(4)?.takeIf { it.isFinite() && it >= 0f } else null
    val fps = stats.getOrNull(0)?.takeIf { it.isFinite() && it >= 0f }
    // Old four-field cores cannot supply a clock reading; they can still supply
    // the optional, clearly approximate FPS comparison. New cores suppress stale
    // FPS across startup/pause/restore until the first fresh clock window.
    val ready = stats.size < 6 || status == 2
    val estimate = if (ready && reference > 0 && fps != null) 100f * fps / reference else null
    return Reading(clock, estimate?.takeIf { it.isFinite() }, reference, paused)
  }

  fun percent(value: Float?): String =
    if (value != null && value.isFinite() && value >= 0f) String.format(Locale.ROOT, "%.0f%%", value) else "--"
}
