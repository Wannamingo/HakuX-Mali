package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class PostShadersTest {
  private fun options(mode: Int, vararg values: Pair<ExperienceSetting, Int>) =
    ExperienceOptions(mapOf(ExperienceSetting.POST_SHADER to mode) + values)

  @Test fun presetsKeepTheirCharacterAndProduceExactlyEightNativeValues() {
    assertArrayEquals(intArrayOf(1, 1, 1, 14, 24, 0, 0, 0), PostShaders.controls(options(1)))
    assertArrayEquals(intArrayOf(2, 1, 1, 18, 30, 12, 0, 6), PostShaders.controls(options(2)))
    assertArrayEquals(intArrayOf(4, 1, 1, 14, 24, 0, 0, 3), PostShaders.controls(options(4)))
    assertEquals("postshader=2,1,1,18,30,12,0,6", PostShaders.argument(options(2)))
  }

  @Test fun explicitZeroIsDistinctFromPresetDefault() {
    val result = PostShaders.controls(options(2, ExperienceSetting.POST_MASK to 0,
      ExperienceSetting.POST_MASK_STRENGTH to 0, ExperienceSetting.POST_SCANLINES to 0,
      ExperienceSetting.POST_GLOW to 0, ExperienceSetting.POST_CURVATURE to 0))
    assertArrayEquals(intArrayOf(2, 0, 1, 0, 0, 0, 0, 0), result)
  }

  @Test fun everyMaskAndSizeSurvivesWithoutChangingOtherControls() {
    for (mask in 0..5) for (size in 1..8) {
      val controls = PostShaders.controls(options(1, ExperienceSetting.POST_MASK to mask,
        ExperienceSetting.POST_MASK_SIZE to size))
      assertArrayEquals(intArrayOf(1, mask, size, 14, 24, 0, 0, 0), controls)
    }
  }

  @Test fun offDoesNotLoseOverridesAndDoesNotInvalidateExistingCalibration() {
    val custom = arrayOf(ExperienceSetting.POST_MASK to 3, ExperienceSetting.POST_BLUR to 27)
    assertEquals("", PostShaders.calibrationSuffix(options(0, *custom)))
    assertArrayEquals(intArrayOf(2, 3, 1, 18, 30, 12, 27, 6), PostShaders.controls(options(2, *custom)))
    assertNotEquals(PostShaders.calibrationSuffix(options(2)), PostShaders.calibrationSuffix(options(2, *custom)))
    assertTrue(PostShaders.calibrationSuffix(options(2)).startsWith("|postshader=v2:"))
  }

  @Test fun malformedValuesAreBoundedAndSentinelsNeverReachNative() {
    val raw = PostShaders.fields.associateWith { Int.MAX_VALUE } + (ExperienceSetting.POST_SHADER to 4)
    assertArrayEquals(intArrayOf(4, 5, 8, 100, 80, 30, 100, 30), PostShaders.controls(ExperienceOptions(raw)))
    for (mode in 0..4) {
      val controls = PostShaders.controls(options(mode))
      assertEquals(8, controls.size)
      assertTrue(controls.all { it >= 0 })
    }
    assertTrue(ExperienceSetting.POST_MASK.accepts(-1))
    assertFalse(ExperienceSetting.POST_MASK.accepts(-2))
    assertFalse(ExperienceSetting.POST_MASK_SIZE.accepts(9))
  }

  @Test fun draftChangesAreDeferredAndCanRestorePresetOrGlobalValues() {
    val base = options(2, ExperienceSetting.POST_MASK to 5, ExperienceSetting.POST_BLUR to 70)
    val global = options(1, ExperienceSetting.POST_MASK to 2)
    val draft = PostShaderDraft(base, global)
    assertTrue(draft.changes.isEmpty())
    draft.set(ExperienceSetting.POST_MASK, 3)
    assertEquals(5, base[ExperienceSetting.POST_MASK])
    assertEquals(3, draft.options()[ExperienceSetting.POST_MASK])
    draft.preset()
    assertEquals(2, draft.options()[ExperienceSetting.POST_SHADER])
    assertArrayEquals(PostShaders.controls(options(2)), PostShaders.controls(draft.options()))
    draft.inherit()
    assertTrue(draft.changes.values.all { it == null })
    assertArrayEquals(PostShaders.controls(global), PostShaders.controls(draft.options()))
  }

  @Test fun invalidDraftFieldCannotAlterOtherSettings() {
    val draft = PostShaderDraft(options(1), ExperienceOptions())
    assertThrows(IllegalArgumentException::class.java) { draft.set(ExperienceSetting.VOLUME, 0) }
    assertTrue(draft.changes.isEmpty())
  }
}
