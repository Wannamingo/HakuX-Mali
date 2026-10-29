package com.rfandango.haku_x

import org.junit.Assert.*
import org.junit.Test

class TemperaturePolicyTest {
  @Test fun thermalAndBatteryUnits() {
    assertEquals(46.230f, TemperaturePolicy.milliDegrees("46230\n")!!, 0.001f)
    assertEquals(30.5f, TemperaturePolicy.batteryTenths(305)!!, 0.001f)
    assertNull(TemperaturePolicy.milliDegrees("Permission denied"))
    assertNull(TemperaturePolicy.milliDegrees("2147483647"))
    assertNull(TemperaturePolicy.batteryTenths(Int.MIN_VALUE))
  }

  @Test fun vitaSensorNamesStayDistinct() {
    for (name in listOf("bigcore-thermal", "little-core-thermal", "cpu-thermal"))
      assertEquals(TemperaturePolicy.Kind.CPU, TemperaturePolicy.kind(name))
    assertEquals(TemperaturePolicy.Kind.GPU, TemperaturePolicy.kind("gpu-thermal"))
    assertEquals(TemperaturePolicy.Kind.SOC, TemperaturePolicy.kind("soc-thermal"))
    assertEquals(TemperaturePolicy.Kind.BATTERY, TemperaturePolicy.kind("cw221X-bat"))
    assertNull(TemperaturePolicy.kind("ddr-thermal"))
    assertNull(TemperaturePolicy.kind("npu-thermal"))
  }

  @Test fun hottestCpuAndBatteryFallback() {
    val samples = listOf(
      TemperaturePolicy.Sample(TemperaturePolicy.Kind.CPU, 45f),
      TemperaturePolicy.Sample(TemperaturePolicy.Kind.CPU, 48f),
      TemperaturePolicy.Sample(TemperaturePolicy.Kind.GPU, 46f),
      TemperaturePolicy.Sample(TemperaturePolicy.Kind.SOC, 47f),
      TemperaturePolicy.Sample(TemperaturePolicy.Kind.BATTERY, 31f),
    )
    val r = TemperaturePolicy.readings(samples, 30.5f)
    assertEquals(48f, r.cpu!!, 0.001f)
    assertEquals(46f, r.gpu!!, 0.001f)
    assertEquals(30.5f, r.battery!!, 0.001f)
    assertEquals(31f, TemperaturePolicy.readings(samples, null).battery!!, 0.001f)
    val batteryOnly = TemperaturePolicy.readings(emptyList(), 30f)
    assertNull(batteryOnly.cpu)
    assertNull(batteryOnly.gpu)
    assertFalse(batteryOnly.hudText().contains("CPU"))
    assertFalse(batteryOnly.hudText().contains("GPU"))
    assertEquals(" | TEMP --", TemperaturePolicy.readings(emptyList(), null).hudText())
  }
}
