package com.rfandango.haku_x

import org.junit.Assert.*
import org.junit.Test

class GpuDriverPolicyTest {
  @Test fun t820MarketingNamesDoNotEnableOtherUnisocChips() {
    for (name in listOf("T820", "Unisoc T820", "UNISOC_T820", "Unisoc Tiger T820", "Spreadtrum T820", "Tiger T820"))
      assertTrue(name, GpuDriverPolicy.isUnisocT820(name))
    for (name in listOf("", "unknown", "T618", "T606", "T760", "T830", "T8200", "Mali-G57", "ums9620", "RK3576"))
      assertFalse(name, GpuDriverPolicy.isUnisocT820(name))
    assertTrue(GpuDriverPolicy.isUnisocT820("unknown", "sprd", "Unisoc T820"))
  }

  @Test fun rg556ModelAndVendorFallback() {
    for (name in listOf("RG556", "RG 556", "rg-556", "ANBERNIC RG556"))
      assertTrue(name, GpuDriverPolicy.isRg556(name))
    assertTrue(GpuDriverPolicy.isRg556("GammaOS Full", "generic_arm64", "RG556"))
    for (name in listOf("RG556 Plus", "RG557", "RG476H", "T820", "Mali-G57", "unknown rg556 device"))
      assertFalse(name, GpuDriverPolicy.isRg556(name))
  }

  @Test fun chipFamilyUnifiedAvailabilityUsesSeparateApiFloors() {
    for (sdk in 28..36) {
      assertEquals("T820 API $sdk", sdk >= 33,
        GpuDriverPolicy.offersUnifiedBundle(sdk, false, false, unisocT820Mali = true))
      assertEquals("Dimensity 8300 API $sdk", sdk >= 34,
        GpuDriverPolicy.offersUnifiedBundle(sdk, false, false, dimensity8300Mali = true))
    }
  }

  @Test fun newlyEnabledChipFamiliesKeepManualSystemAndCustomChoices() {
    val automatic = GpuDriverPolicy.allowsAutomaticBundle(false, false)
    assertFalse(automatic)
    for (bootstrapped in listOf(false, true))
      for (setup in listOf(false, true))
        for (active in listOf(false, true))
          for (identical in listOf(false, true))
            assertFalse(GpuDriverPolicy.selectBundled(bootstrapped, setup, active, identical, automatic))
    assertTrue(GpuDriverPolicy.allowsAutomaticBundle(true, false))
    assertTrue(GpuDriverPolicy.allowsAutomaticBundle(false, true))
  }

  @Test fun unifiedBundleUsesQualifiedAndroidFloors() {
    assertFalse(GpuDriverPolicy.offersUnifiedBundle(32, true, false))
    assertTrue(GpuDriverPolicy.offersUnifiedBundle(33, true, false))
    assertTrue(GpuDriverPolicy.offersUnifiedBundle(34, true, false))
    assertFalse(GpuDriverPolicy.offersUnifiedBundle(33, false, true))
    assertTrue(GpuDriverPolicy.offersUnifiedBundle(34, false, true))
  }

  @Test fun unifiedBundleIsNotOfferedToOtherMaliDevices() {
    for (sdk in 28..36) assertFalse(GpuDriverPolicy.offersUnifiedBundle(sdk, false, false))
  }

  @Test fun experimentalRg476hKeepsSystemDefault() {
    assertFalse(GpuDriverPolicy.selectBundled(false, false, false, false, allowAutoSelection = false))
    assertFalse(GpuDriverPolicy.selectBundled(false, true, true, true, allowAutoSelection = false))
  }

  @Test fun rg476hExactModelAliases() {
    for (name in listOf("RG 476H", "RG476H", "rg-476h"))
      assertTrue(name, GpuDriverPolicy.isRg476h(name))
    for (name in listOf("RG477M", "RG476H Plus", "RG476", "T820", "ums9620", "Mali-G57"))
      assertFalse(name, GpuDriverPolicy.isRg476h(name))
    assertTrue(GpuDriverPolicy.isRg476h("GammaOS Full", "RG 476H"))
  }

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
