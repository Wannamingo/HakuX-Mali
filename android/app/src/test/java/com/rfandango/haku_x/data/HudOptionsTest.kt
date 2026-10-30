package com.rfandango.haku_x.data

import com.rfandango.haku_x.TemperaturePolicy
import org.junit.Assert.*
import org.junit.Test

class HudOptionsTest {
  @Test fun invalidAppearanceCannotCreateAnInvisibleOrOversizedHud() {
    val options = HudOptions(textSize = Int.MAX_VALUE, background = -4).sanitized()
    assertEquals(18, options.textSize)
    assertEquals(0, options.background)
  }

  @Test fun individualSensorsCanBeHiddenWithoutInventingMissingReadings() {
    val readings = TemperaturePolicy.Readings(null, 45f, 42f, 31f)
    assertEquals("", readings.hudText(false, false, false))
    assertEquals(" | BAT 31°C", readings.hudText(false, false, true))
    assertEquals(" | SOC 42°C", readings.hudText(true, false, false))
    assertFalse(HudOptions(cpu = false, gpu = false, battery = false).temperatures)
  }
}
