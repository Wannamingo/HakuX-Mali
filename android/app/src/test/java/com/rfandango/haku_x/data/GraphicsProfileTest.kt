package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class GraphicsProfileTest {
  @Test fun fpsOverrideInheritsAndInvalidValuesFallBackToGlobal() {
    assertEquals(120, effectiveFpsLimit(null, 120))
    assertEquals(60, effectiveFpsLimit("60", 120))
    assertEquals(0, effectiveFpsLimit("0", 120))
    for (invalid in listOf("-1", "999", "no", "99999999999"))
      assertEquals(90, effectiveFpsLimit(invalid, 90))
  }

  @Test fun profileLabelsFollowActualValuesAfterManualChanges() {
    val quality = GraphicsProfile.QUALITY.options
    assertEquals(GraphicsProfile.QUALITY, GraphicsProfile.matching(quality))
    assertNull(GraphicsProfile.matching(quality.copy(fpsLimit = 90)))
    assertNull(GraphicsProfile.matching(quality.copy(scale = 3)))
  }

  @Test fun balancedAndQualityKeepThe120FpsLimit() {
    assertEquals(120, GraphicsProfile.BALANCED.options.fpsLimit)
    assertEquals(120, GraphicsProfile.QUALITY.options.fpsLimit)
    assertTrue(GraphicsProfile.PERFORMANCE.options.scale <= GraphicsProfile.QUALITY.options.scale)
    for (control in PictureControl.entries) {
      assertEquals(100, control.sanitize(100))
      assertTrue(control.minimum <= control.sanitize(Int.MIN_VALUE))
      assertTrue(control.maximum >= control.sanitize(Int.MAX_VALUE))
    }
  }
}
