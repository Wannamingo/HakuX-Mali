package com.rfandango.haku_x

/** Device detection recommends defaults; manual packages are never filtered by GPU or model. */
object GpuDriverPolicy {
  const val MALISX2_LIBRARY = "libvulkan_malisx2.so"
  enum class Chip { UNISOC_T820, UNISOC_T618, DIMENSITY_8300 }

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

  fun isUnisocT618(vararg names: String): Boolean = names.any {
    val name = it.lowercase().replace(Regex("[^a-z0-9]"), "")
      .removePrefix("unisoc").removePrefix("spreadtrum").removePrefix("tiger")
    name == "t618"
  }

  /** A generic UMS512 may be another chip; require a known T618 model as well. */
  fun isT618Handheld(boardPlatform: String, vararg names: String): Boolean =
    boardPlatform.equals("ums512", ignoreCase = true) && names.any {
      it.lowercase().replace(Regex("[^a-z0-9]"), "").removePrefix("anbernic") in
        setOf("rg405m", "rg505", "rgrotate")
    }

  fun chip(t820: Boolean, t618: Boolean, dimensity8300: Boolean): Chip? = when {
    t820 -> Chip.UNISOC_T820
    t618 -> Chip.UNISOC_T618
    dimensity8300 -> Chip.DIMENSITY_8300
    else -> null
  }

  /** Displaying a package never bypasses the requirements of the actual binary. */
  fun canLoadPackage(sdk: Int, minApi: Int): Boolean = minApi >= 0 && sdk >= minApi

  /** Keep the historical automatic default only on the original qualified devices. */
  fun allowsAutomaticBundle(rg477mMali: Boolean, vitaProMali: Boolean): Boolean =
    rg477mMali || vitaProMali

  /** Keep explicit system/custom choices; migrate an identical imported binary only once. */
  fun selectBundled(bootstrapped: Boolean, setupComplete: Boolean, hasActive: Boolean, identical: Boolean,
                    allowAutoSelection: Boolean = true): Boolean =
    allowAutoSelection && !bootstrapped && (identical || (!setupComplete && !hasActive))

  /** A single safe filename for the native driver's pipe-delimited loader argument. */
  fun acceptsLibrary(library: String?): Boolean =
    library != null && Regex("[A-Za-z0-9_.-]+\\.so").matches(library)

  fun isDriverLoadFailure(message: String): Boolean = message.startsWith("The selected Vulkan driver") ||
    message.startsWith("This build cannot load custom Vulkan drivers")
}
