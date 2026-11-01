package com.rfandango.haku_x

import com.rfandango.haku_x.data.RumbleMode
import com.rfandango.haku_x.data.RumbleSafety
import org.junit.Assert.*
import org.junit.Test

class RumbleSafetyTest {
  private val pad = "01a85ba7044c3d3c96030d501b88787e6aeb7b9a"
  private fun oldPad(build: String = "eng.gamma.20251222.175256", descriptor: String = pad,
                     vendor: Int = 0x045e, product: Int = 0x02fd, name: String = "Xbox Wireless Controller") =
    RumbleSafety.legacyGammaPad("", build, name, descriptor, vendor, product)

  @Test fun observedGammaPadIsProtectedWithoutAProfileMigration() {
    assertTrue(oldPad())
    assertFalse(RumbleSafety.enabled(RumbleMode.AUTO, oldPad()))
  }
  @Test fun stockAndNewGammaBuildsKeepRumble() {
    for (build in listOf("Anbernic", "eng.gamma.20260726.120000", "unknown", "")) {
      assertFalse(oldPad(build))
      assertTrue(RumbleSafety.enabled(RumbleMode.AUTO, oldPad(build)))
    }
  }
  @Test fun externalControllersAreNotBlanketDisabledOnGammaOs() {
    assertFalse(oldPad(descriptor = "different-bluetooth-unit"))
    assertFalse(oldPad(vendor = 0x054c))
    assertFalse(oldPad(product = 0x02ea))
    assertFalse(oldPad(name = "Other controller"))
  }
  @Test fun displayCanIdentifyTheObservedBuild() {
    assertTrue(RumbleSafety.legacyGammaPad("lineage eng.gamma.20251222.175256 test-keys", "", "Xbox Wireless Controller", pad, 0x045e, 0x02fd))
  }
  @Test fun explicitOffPreventsRumbleOnAnyFirmware() {
    assertFalse(RumbleSafety.enabled(RumbleMode.OFF, true))
    assertFalse(RumbleSafety.enabled(RumbleMode.OFF, false))
  }
  @Test fun explicitOnAllowsRetestingAfterAFirmwareFix() {
    assertTrue(RumbleSafety.enabled(RumbleMode.ON, true))
    assertTrue(RumbleSafety.enabled(RumbleMode.ON, false))
  }
}
