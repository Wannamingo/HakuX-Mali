package com.rfandango.haku_x.data
import org.junit.Assert.*
import org.junit.Test

class RendererCapabilitiesTest {
  @Test fun deviceLimitsRestrictChoices() {
    assertEquals(listOf(0),RendererCapabilities.bounded(1).anisotropyChoices)
    assertEquals(listOf(0,2,4),RendererCapabilities.bounded(4).anisotropyChoices)
    assertEquals(listOf(0,2,4,8,16),RendererCapabilities.bounded(16).anisotropyChoices)
    assertEquals(listOf(0),RendererCapabilities.bounded(-1).anisotropyChoices)
  }
  @Test fun importingUnknownAnisotropyNeverEnablesAnInvalidValue() {
    assertFalse(ExperienceSetting.ANISOTROPY.accepts(3))
    assertFalse(ExperienceSetting.ANISOTROPY.accepts(Int.MAX_VALUE))
    assertTrue(ExperienceSetting.ANISOTROPY.accepts(8))
    assertTrue(ExperienceSetting.ANISOTROPY.accepts(16))
    assertEquals(0,ExperienceOptions(mapOf(ExperienceSetting.ANISOTROPY to 3))[ExperienceSetting.ANISOTROPY])
  }
}
