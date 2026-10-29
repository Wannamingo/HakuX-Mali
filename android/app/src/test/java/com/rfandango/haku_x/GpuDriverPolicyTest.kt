package com.rfandango.haku_x

import org.junit.Assert.*
import org.junit.Test

class GpuDriverPolicyTest {
  @Test fun vitaProAliasesAndOtherHandhelds() {
    for (name in listOf("RG VITA Pro", "RG-Vita-Pro", "rgvitapro"))
      assertTrue(name, GpuDriverPolicy.isRgVitaPro(name))
    for (name in listOf("RG VITA", "RG477M", "RK3576", "RK3566", "GammaOS Next Lite", "other rgvitapro device"))
      assertFalse(name, GpuDriverPolicy.isRgVitaPro(name))
    assertTrue(GpuDriverPolicy.isRgVitaPro("GammaOS Next Lite", "RG VITA Pro"))
  }

  @Test fun dimensityAliasesAndOtherChips() {
    for (name in listOf("MT6897", "mt6897", "Dimensity 8300", "MediaTek Dimensity 8300 Ultra"))
      assertTrue(name, GpuDriverPolicy.isDimensity8300(name))
    for (name in listOf("MT6895", "MT68970", "MT6989", "Dimensity 8200", "Mali-G615", "GammaOS Full"))
      assertFalse(name, GpuDriverPolicy.isDimensity8300(name))
    assertTrue(GpuDriverPolicy.isRg477m("GammaOS Full", "RG477M"))
  }

  @Test fun incompatibleAndUnsafeLibraries() {
    assertTrue(GpuDriverPolicy.acceptsLibrary("libvulkan_malisx2.so", false, true))
    assertFalse(GpuDriverPolicy.acceptsLibrary("libvulkan_malisx2.so", true, false))
    assertFalse(GpuDriverPolicy.acceptsLibrary("libvulkan_freedreno.so", false, true))
    assertFalse(GpuDriverPolicy.acceptsLibrary("libvulkan_malisx2.so", false, false))
    for (name in listOf(null, "", "../libvulkan_malisx2.so", "/data/driver.so", "x|y.so"))
      assertFalse(GpuDriverPolicy.acceptsLibrary(name, false, true))
  }

  @Test fun firstInstallAndExistingChoices() {
    assertTrue(GpuDriverPolicy.selectBundled(false, false, false, false))
    assertTrue(GpuDriverPolicy.selectBundled(false, true, true, true))
    assertFalse(GpuDriverPolicy.selectBundled(false, true, false, false))
    assertFalse(GpuDriverPolicy.selectBundled(false, false, true, false))
    assertFalse(GpuDriverPolicy.selectBundled(true, false, false, false))
    assertFalse(GpuDriverPolicy.selectBundled(true, true, true, true))
  }
}
