package com.rfandango.haku_x

/** Standalone JVM smoke test: compile together with GpuDriverPolicy.kt. */
fun main() {
  check(GpuDriverPolicy.isRg477m("ANBERNIC RG477M"))
  check(GpuDriverPolicy.isRg477m("RG 477M"))
  check(GpuDriverPolicy.isRg477m("unknown", "rg477m", "unknown"))
  check(!GpuDriverPolicy.isRg477m("GammaOS Full", "tdgsi_arm64_ab", "lineage_arm64_bgN"))
  check(GpuDriverPolicy.isRg477m("RG477M")) // ro.product.vendor.model on GammaOS
  check(!GpuDriverPolicy.isRg477m("RG477V", "RG557", "RG476H"))
  check(GpuDriverPolicy.acceptsLibrary("libvulkan_malisx2.so", false, true))
  check(!GpuDriverPolicy.acceptsLibrary("libvulkan_freedreno.so", false, true))
  check(!GpuDriverPolicy.acceptsLibrary("libvulkan_malisx2.so", true, false))
  check(!GpuDriverPolicy.acceptsLibrary("libvulkan_malisx2.so", false, false))
  check(GpuDriverPolicy.acceptsLibrary("vulkan.adreno.so", true, false))
  check(GpuDriverPolicy.acceptsLibrary("libvulkan_freedreno.so", true, false))
  for (name in listOf(null, "", "../libvulkan_malisx2.so", "/tmp/driver.so", "dir/driver.so", "x|y.so", "driver.dll")) {
    check(!GpuDriverPolicy.acceptsLibrary(name, true, false))
    check(!GpuDriverPolicy.acceptsLibrary(name, false, true))
  }
  println("PASS GPU family selection, RG477M model aliases, library validation")
}
