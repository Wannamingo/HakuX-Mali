package com.rfandango.haku_x.data

import kotlin.math.abs

data class CalibrationMetrics(
  val seconds: Double, val frames: Long, val samples: Int,
  val medianMs: Double, val p95Ms: Double, val maxMs: Double,
  val cpuMs: Double, val peakRssMb: Double, val gpuMs: Double = -1.0,
  val gpuBuffers: Long = 0, val temperatureCpu: Double? = null,
  val temperatureGpu: Double? = null, val temperatureBattery: Double? = null,
) {
  val fps get() = if (seconds > 0) frames / seconds else 0.0
  val valid get() = seconds >= 9 && frames >= 100 && samples >= 60 &&
    listOf(seconds, medianMs, p95Ms, maxMs, cpuMs, peakRssMb).all { it.isFinite() } &&
    medianMs > 0 && p95Ms >= medianMs && maxMs >= p95Ms && maxMs < 3000 && cpuMs >= 0 && peakRssMb > 0
}

data class CalibrationRun(val options: GraphicsOptions, val metrics: CalibrationMetrics?, val error: String? = null)

object CalibrationPolicy {
  const val WARMUP_MS = 10_000L
  fun measurementMs(index: Int): Long = if (index == 0) 15_000L else 10_000L
  const val BOOT_TIMEOUT_MS = 60_000L
  const val SCHEMA = 1

  /** Common FPS ceiling keeps this a graphics comparison, not a comparison of limiters. */
  fun candidates(manual: GraphicsOptions, ceiling: Int): List<GraphicsOptions> {
    val fps = ceiling.coerceIn(24,120)
    val baseline = GraphicsOptions(1,false,"bilinear",fps)
    val choices = listOf(baseline, manual.copy(fpsLimit=fps),
      GraphicsOptions(2,false,"bilinear",fps), GraphicsOptions(1,false,"sgsr",fps)).distinct()
    return choices + baseline // Repeat the reference to detect drift/thermal throttling.
  }

  /** No measured profile is published if the reference scene changed materially. */
  fun referenceStable(runs: List<CalibrationRun>): Boolean {
    if (runs.size < 2 || runs.first().options != runs.last().options) return false
    val first = runs.first().metrics?.takeIf { it.valid } ?: return false
    val last = runs.last().metrics?.takeIf { it.valid } ?: return false
    return abs(last.fps-first.fps) / first.fps.coerceAtLeast(1.0) <= .10 &&
      abs(last.p95Ms-first.p95Ms) / first.p95Ms.coerceAtLeast(1.0) <= .25
  }

  fun winner(runs: List<CalibrationRun>): CalibrationRun? {
    if (!referenceStable(runs)) return null
    val valid = runs.dropLast(1).filter { it.error == null && it.metrics?.valid == true }
    val bestFps = valid.maxOfOrNull { it.metrics!!.fps } ?: return null
    val near = valid.filter { it.metrics!!.fps >= bestFps * .97 }
    val bestP95 = near.minOf { it.metrics!!.p95Ms }
    return near.filter { it.metrics!!.p95Ms <= bestP95 * 1.05 + .3 }.minWithOrNull(
      compareBy<CalibrationRun> { it.options.scale }
        .thenBy { if (it.options.fxaa) 1 else 0 }
        .thenBy { if (it.options.upscale == "bilinear") 0 else 1 }
        .thenBy { it.metrics!!.cpuMs / it.metrics.frames.coerceAtLeast(1) })
  }
}
