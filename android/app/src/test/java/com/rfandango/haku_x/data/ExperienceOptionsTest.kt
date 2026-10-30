package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class ExperienceOptionsTest {
  @Test fun defaultsPreserveOutput() {
    val value = ExperienceOptions()
    assertEquals("audio=100,0,1,1", value.audioArgument())
    assertEquals("framing=0,0,0,0,100,0,0", value.framingArgument())
    assertFalse(value.enabled(ExperienceSetting.AUTO_SAVE))
    assertFalse(value.enabled(ExperienceSetting.THERMAL_WARNING))
    assertFalse(value.enabled(ExperienceSetting.AUTO_PERFORMANCE))
  }
  @Test fun automaticPolicyUsesTheDeviceDisplayWithinTheAppCeiling() {
    assertEquals(GraphicsOptions(1,false,"bilinear",120), AutomaticPerformance.choose(listOf(60f,120f,240f)))
    assertEquals(60,AutomaticPerformance.choose(listOf(59.94f)).fpsLimit)
    assertEquals(90,AutomaticPerformance.choose(listOf(60f,90f)).fpsLimit)
  }
  @Test fun automaticPolicyHasASafeFallbackForMissingDisplayData() {
    assertEquals(60,AutomaticPerformance.choose(emptyList()).fpsLimit)
    assertEquals(60,AutomaticPerformance.choose(listOf(Float.NaN,Float.POSITIVE_INFINITY)).fpsLimit)
  }
  @Test fun invalidValuesAreBounded() {
    val value = ExperienceOptions(mapOf(ExperienceSetting.VOLUME to -1, ExperienceSetting.ZOOM to Int.MAX_VALUE,
      ExperienceSetting.CROP_LEFT to 95, ExperienceSetting.SHIFT_X to -100))
    assertEquals(0, value[ExperienceSetting.VOLUME]); assertEquals(200, value[ExperienceSetting.ZOOM])
    assertEquals(15, value[ExperienceSetting.CROP_LEFT]); assertEquals(-20, value[ExperienceSetting.SHIFT_X])
  }
  @Test fun stickGainAndInversionPreserveDeadzone() {
    assertEquals(0f, StickTransform.apply(.1f, .2f, 2f, true), .0001f)
    assertEquals(-.5f, StickTransform.apply(.4f, .2f, 2f, true), .0001f)
    assertEquals(1f, StickTransform.apply(1f, .1f, 2f), .0001f)
    assertEquals(-1f, StickTransform.apply(-1f, .1f, 2f), .0001f)
  }
  @Test fun malformedStickValuesNeverReachNative() {
    assertEquals(0f, StickTransform.apply(Float.NaN, 0f), .0001f)
    assertTrue(StickTransform.apply(.5f, Float.POSITIVE_INFINITY, Float.NaN).isFinite())
  }
  @Test fun shortcutDoesNotSendSelectToGameAndTriggersOnRelease() {
    val gate = HotkeyGate(); val bindings = mapOf(HotkeyAction.MENU to 108)
    assertTrue(gate.event(109, true, false, bindings).consume)
    assertNull(gate.event(108, true, false, bindings).action)
    assertNull(gate.event(108, true, true, bindings).action)
    assertEquals(HotkeyAction.MENU, gate.event(108, false, false, bindings).action)
    val release = gate.event(109, false, false, bindings)
    assertTrue(release.consume); assertFalse(release.flushModifier)
  }
  @Test fun selectTapStillReachesGame() {
    val gate = HotkeyGate(); val bindings = mapOf(HotkeyAction.MENU to 108)
    gate.event(109, true, false, bindings)
    val result = gate.event(109, false, false, bindings)
    assertTrue(result.flushModifier); assertFalse(result.consume)
  }
  @Test fun unboundChordFlushesSelectBeforeButton() {
    val gate = HotkeyGate(); val bindings = mapOf(HotkeyAction.MENU to 108)
    gate.event(109, true, false, bindings)
    val button = gate.event(96, true, false, bindings)
    assertTrue(button.flushModifier); assertFalse(button.consume)
    assertFalse(gate.event(109, false, false, bindings).consume)
  }
  @Test fun releasingModifierFirstStillFiresOnlyOnce() {
    val gate = HotkeyGate(); val bindings = mapOf(HotkeyAction.SCREENSHOT to 100)
    gate.event(109, true, false, bindings); gate.event(100, true, false, bindings)
    assertTrue(gate.event(109, false, false, bindings).consume)
    assertEquals(HotkeyAction.SCREENSHOT, gate.event(100, false, false, bindings).action)
    assertNull(gate.event(100, false, false, bindings).action)
  }
}
