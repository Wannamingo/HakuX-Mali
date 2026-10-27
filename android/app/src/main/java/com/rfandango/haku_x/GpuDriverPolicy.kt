package com.rfandango.haku_x

/** Keep the experimental MaliSX2 path scoped to the requested handheld. */
object GpuDriverPolicy {
  const val MALISX2_LIBRARY = "libvulkan_malisx2.so"

  fun isRg477m(vararg names: String): Boolean = names.any {
    it.lowercase().replace(Regex("[^a-z0-9]"), "").contains("rg477m")
  }

  fun acceptsLibrary(library: String?, adreno: Boolean, rg477m: Boolean): Boolean {
    if (library == null || !Regex("[A-Za-z0-9_.-]+\\.so").matches(library)) return false
    return if (adreno) library != MALISX2_LIBRARY
    else rg477m && library == MALISX2_LIBRARY
  }
}
