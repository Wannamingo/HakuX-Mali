package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sqrt

class StickPairTest {
  @Test fun heldGasSurvivesSteeringAndUnboundAxesReset() {
    val pad = PadState()
    val profile = PadProfile(mapOf(XInput.A to listOf(Binding.Key(96)),
      XInput.RT to listOf(Binding.Key(105)), XInput.LS_X to listOf(Binding.Axis(0, 1)),
      XInput.LS_Y to listOf(Binding.Axis(1, -1))))
    pad.keys.addAll(listOf(96, 105))
    val out = PadOut()
    for (i in -100..100) {
      pad.axes[0] = i/100f; pad.axes[1] = .3f
      pad.compute(profile, out)
      assertEquals(255, out.analog[0]); assertEquals(255, out.analog[7])
    }
    pad.compute(profile.copy(bind=emptyMap()), out)
    assertEquals(0, out.lx); assertEquals(0, out.ly); assertEquals(0, out.analog[0])
  }
  @Test fun diagonalReachesBothXboxAxes() {
    val d = 1f / sqrt(2f)
    val out = StickTransform.pair(d, d, .1f)
    assertEquals(1f, out.first, .0001f); assertEquals(1f, out.second, .0001f)
    val round = StickTransform.pair(d, d, 0f, square = false)
    assertEquals(d, round.first, .0001f)
  }
  @Test fun deadzoneIsRadialAndContinuous() {
    assertEquals(0f to 0f, StickTransform.pair(.05f, .05f, .1f))
    val out = StickTransform.pair(.08f, .08f, .1f)
    assertTrue(out.first > 0f); assertEquals(out.first, out.second, 0f)
  }
  @Test fun rangeInversionAndLimits() {
    assertEquals(.75f, StickTransform.pair(.5f, 0f, 0f, range=1.5f).first, .0001f)
    assertEquals(-1f, StickTransform.pair(1f, 0f, 0f, range=1.5f, invertX=true).first, 0f)
    assertEquals(0f to 0f, StickTransform.pair(Float.NaN, 0f, 0f))
    for (i in -100..100) for (j in -100..100) {
      val p = StickTransform.pair(i/100f,j/100f,.1f)
      assertTrue(p.first.isFinite() && p.second.isFinite())
      assertTrue(p.first in -1f..1f && p.second in -1f..1f)
    }
  }
}
