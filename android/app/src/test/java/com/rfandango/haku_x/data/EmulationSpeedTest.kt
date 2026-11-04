package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class EmulationSpeedTest {
  @Test fun thirtyFpsGameCanRunAtFullSpeed() {
    val reading=EmulationSpeed.read(floatArrayOf(30f,33.3f,33.3f,0f,100f,2f),30)
    assertEquals(100f,reading.clockPercent!!,0f)
    assertEquals(100f,reading.framePercent!!,0f)
  }
  @Test fun lowFrameRateDoesNotPretendTheClockIsAlsoSlow() {
    val reading=EmulationSpeed.read(floatArrayOf(20f,50f,50f,0f,100f,2f),30)
    assertEquals(100f,reading.clockPercent!!,0f)
    assertEquals(66.6667f,reading.framePercent!!,.001f)
  }
  @Test fun frameTargetIsExplicitAndNotGuessedFromTheFpsLimit() {
    val reading=EmulationSpeed.read(floatArrayOf(30f,33f,33f,0f,80f,2f),0)
    assertEquals(80f,reading.clockPercent!!,0f)
    assertNull(reading.framePercent)
    assertEquals(0,ExperienceSetting.HUD_REFERENCE_FPS.sanitize(31))
    assertEquals(120,ExperienceSetting.HUD_REFERENCE_FPS.sanitize(120))
  }
  @Test fun legacyCoreDoesNotInventAClockReading() {
    val reading=EmulationSpeed.read(floatArrayOf(30f,33f,33f,0f),30)
    assertNull(reading.clockPercent)
    assertEquals(100f,reading.framePercent!!,0f)
    assertEquals("--",EmulationSpeed.percent(reading.clockPercent))
  }
  @Test fun pauseAndFirstFreshWindowHideStaleFrameReadings() {
    for(status in listOf(0f,1f,3f)) {
      val reading=EmulationSpeed.read(floatArrayOf(60f,16f,16f,0f,100f,status),60)
      assertNull(reading.clockPercent)
      assertNull(reading.framePercent)
      assertEquals(status==3f,reading.paused)
    }
  }
  @Test fun invalidAndEmptySamplesAreUnavailable() {
    assertNull(EmulationSpeed.read(floatArrayOf(),30).framePercent)
    for(value in listOf(Float.NaN,Float.POSITIVE_INFINITY,-1f)) {
      val reading=EmulationSpeed.read(floatArrayOf(value,0f,0f,0f,value,2f),60)
      assertNull(reading.clockPercent)
      assertNull(reading.framePercent)
      assertEquals("--",EmulationSpeed.percent(value))
    }
  }
  @Test fun stoppedFramesAndFasterThanReferenceAreNotClamped() {
    assertEquals(0f,EmulationSpeed.read(floatArrayOf(0f,0f,0f,0f,30f,2f),30).framePercent!!,0f)
    assertEquals(200f,EmulationSpeed.read(floatArrayOf(60f,0f,0f,0f,100f,2f),30).framePercent!!,0f)
    assertEquals("67%",EmulationSpeed.percent(66.6667f))
  }
  @Test fun speedDefaultsOnForExistingHudSettings() {
    assertTrue(HudOptions().speed)
    assertFalse(HudOptions(speed=false).sanitized().speed)
  }
}
