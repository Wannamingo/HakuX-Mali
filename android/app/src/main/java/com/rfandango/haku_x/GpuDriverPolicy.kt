package com.rfandango.haku_x

/** MaliSX2 is scoped to the tested handheld and Dimensity 8300/MT6897 family. */
object GpuDriverPolicy {
  const val MALISX2_LIBRARY = "libvulkan_malisx2.so"

  fun isRg477m(vararg names: String): Boolean = names.any {
    it.lowercase().replace(Regex("[^a-z0-9]"), "").contains("rg477m")
  }

  fun isDimensity8300(vararg names: String): Boolean = names.any {
    val name = it.lowercase().replace(Regex("[^a-z0-9]"), "").removePrefix("mediatek")
    name == "mt6897" || name == "dimensity8300" || name == "dimensity8300ultra"
  }

  /** Keep explicit system/custom choices; migrate an identical imported binary only once. */
  fun selectBundled(bootstrapped: Boolean, setupComplete: Boolean, hasActive: Boolean, identical: Boolean): Boolean =
    !bootstrapped && (identical || (!setupComplete && !hasActive))

  fun acceptsLibrary(library: String?, adreno: Boolean, malisx2Device: Boolean): Boolean {
    if (library == null || !Regex("[A-Za-z0-9_.-]+\\.so").matches(library)) return false
    return if (adreno) library != MALISX2_LIBRARY
    else malisx2Device && library == MALISX2_LIBRARY
  }
}
