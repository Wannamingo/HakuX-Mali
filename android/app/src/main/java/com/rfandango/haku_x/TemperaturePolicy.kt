package com.rfandango.haku_x

import java.util.Locale

/** Thermal-zone values are millidegrees; Android battery values are tenths. */
object TemperaturePolicy {
  enum class Kind { CPU, GPU, SOC, BATTERY }
  data class Sample(val kind: Kind, val celsius: Float)
  data class Readings(val cpu: Float?, val gpu: Float?, val soc: Float?, val battery: Float?) {
    fun hudText(showCpu: Boolean = true, showGpu: Boolean = true, showBattery: Boolean = true): String {
      if (!showCpu && !showGpu && !showBattery) return ""
      val values = listOfNotNull(
        if (showCpu) cpu?.let { "CPU" to it } ?: soc?.let { "SOC" to it } else null,
        if (showGpu) gpu?.let { "GPU" to it } else null,
        if (showBattery) battery?.let { "BAT" to it } else null,
      )
      return if (values.isEmpty()) " | TEMP --" else
        " | " + values.joinToString("  ") { (label, c) -> String.format(Locale.ROOT, "%s %.0f°C", label, c) }
    }
  }

  fun kind(type: String): Kind? {
    val name = type.trim().lowercase(Locale.ROOT).replace('_', '-')
    return when {
      "battery" in name || "batt" in name || name.endsWith("-bat") -> Kind.BATTERY
      "gpu" in name || "mali" in name -> Kind.GPU
      "cpu" in name || "bigcore" in name || "littlecore" in name ||
        "big-core" in name || "little-core" in name -> Kind.CPU
      name == "soc" || name.startsWith("soc-") -> Kind.SOC
      else -> null
    }
  }

  fun milliDegrees(text: String): Float? = text.trim().toIntOrNull()
    ?.takeIf { it in -40_000..150_000 }?.let { it / 1000f }

  fun batteryTenths(raw: Int): Float? = raw.takeIf { it in -400..900 }?.let { it / 10f }

  fun readings(samples: List<Sample>, androidBattery: Float?): Readings {
    fun hottest(kind: Kind) = samples.filter { it.kind == kind }.maxOfOrNull { it.celsius }
    return Readings(hottest(Kind.CPU), hottest(Kind.GPU), hottest(Kind.SOC),
      androidBattery ?: hottest(Kind.BATTERY))
  }
}
