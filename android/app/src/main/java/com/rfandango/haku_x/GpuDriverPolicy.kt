package com.rfandango.haku_x

/** MaliSX2 loading for identified handhelds and SoCs; generic Mali matching is avoided. */
object GpuDriverPolicy {
  const val MALISX2_LIBRARY = "libvulkan_malisx2.so"

  fun isRg477m(vararg names: String): Boolean = names.any {
    it.lowercase().replace(Regex("[^a-z0-9]"), "").contains("rg477m")
  }

  fun isDimensity8300(vararg names: String): Boolean = names.any {
    val name = it.lowercase().replace(Regex("[^a-z0-9]"), "").removePrefix("mediatek")
    name == "mt6897" || name == "dimensity8300" || name == "dimensity8300ultra"
  }

  fun isRgVitaPro(vararg names: String): Boolean = names.any {
    it.lowercase().replace(Regex("[^a-z0-9]"), "") == "rgvitapro"
  }

  fun isRg476h(vararg names: String): Boolean = names.any {
    it.lowercase().replace(Regex("[^a-z0-9]"), "") == "rg476h"
  }

  /** GammaOS can expose T770 as the SoC name on this known T820 handheld. */
  fun isRg476hT820(boardPlatform: String, vararg names: String): Boolean =
    boardPlatform.equals("ums9620", ignoreCase = true) && isRg476h(*names)

  fun isRg556(vararg names: String): Boolean = names.any {
    it.lowercase().replace(Regex("[^a-z0-9]"), "").removePrefix("anbernic") == "rg556"
  }

  fun isUnisocT820(vararg names: String): Boolean = names.any {
    val name = it.lowercase().replace(Regex("[^a-z0-9]"), "")
      .removePrefix("unisoc").removePrefix("spreadtrum").removePrefix("tiger")
    name == "t820"
  }

  /** Chip-family availability; hardware qualification remains device/firmware-specific. */
  fun offersUnifiedBundle(sdk: Int, rg476hMali: Boolean, rg477mMali: Boolean,
                          dimensity8300Mali: Boolean = false, unisocT820Mali: Boolean = false): Boolean =
    (sdk >= 33 && (rg476hMali || unisocT820Mali)) ||
      (sdk >= 34 && (rg477mMali || dimensity8300Mali))

  /** Keep the historical automatic default only on the original qualified devices. */
  fun allowsAutomaticBundle(rg477mMali: Boolean, vitaProMali: Boolean): Boolean =
    rg477mMali || vitaProMali

  /** Keep explicit system/custom choices; migrate an identical imported binary only once. */
  fun selectBundled(bootstrapped: Boolean, setupComplete: Boolean, hasActive: Boolean, identical: Boolean,
                    allowAutoSelection: Boolean = true): Boolean =
    allowAutoSelection && !bootstrapped && (identical || (!setupComplete && !hasActive))

  fun acceptsLibrary(library: String?, adreno: Boolean, malisx2Device: Boolean): Boolean {
    if (library == null || !Regex("[A-Za-z0-9_.-]+\\.so").matches(library)) return false
    return if (adreno) library != MALISX2_LIBRARY
    else malisx2Device && library == MALISX2_LIBRARY
  }
}
