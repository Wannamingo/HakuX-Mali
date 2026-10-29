# RG VITA Pro — experimental Android build

Target tested on 2026-10-29: ANBERNIC RG VITA Pro, Rockchip RK3576,
Mali-G52, Android 14 / GammaOS Next Lite, kernel 6.1.141, arm64.
The vendor model identifies the console even though the system model is
`GammaOS Next Lite`. Games are in the SD card's `ROMs/xbox` SAF tree.

This adaptation is based on the published `v1.2.0-rg477m.2` source.
The separate `1.2.0-rg477m.2-vita.2` APK is published as an additional asset
on that release page; the original RG477M APK and tag are retained.

## Vulkan startup

The unmodified .2 APK failed at `vkCreateDevice` with
`VK_ERROR_FEATURE_NOT_PRESENT` on the system Arm g25p0 driver. Its Vulkan
1.3.283 feature report has runtimeDescriptorArray,
descriptorBindingPartiallyBound, descriptorBindingSampledImageUpdateAfterBind
and shaderSampledImageArrayNonUniformIndexing all disabled. The backend
correctly chose its descriptor-set fallback, but still requested all four
optional Vulkan 1.2 features when creating a Vulkan 1.3 device.

The feature request now follows the selected bindless path. The existing
fallback is used when those features or their descriptor limits are absent.
The system driver then created the device successfully and DOA Xtreme Beach
Volleyball reached its introductory sequence/title at approximately 60 FPS.
This does not establish gameplay performance for every title.

## MaliSX2

The device policy now recognizes the exact RG VITA Pro model together with
RK3576 and a Mali device node. It does not enable every Rockchip or Mali
device. The existing official bundled MaliSX2 0.2.3 includes its Mali-G52
(architecture 7) backend; the driver archive is unchanged.

Actual game logs confirmed Mali-G52 with
`v1.r44p1-malisx2.0.2.3.s22f19f5370b1`, Vulkan 1.1.363, the render-pass
compatibility path and ordered Vulkan fences. DOA's introductory video and
save-selection menu were rendered, and the on-screen Start button worked.
Guest RAM import is unavailable on this driver/device combination, so the
backend uses its existing copy fallback. The system driver supports AHB
import. Longer gameplay and a broad game list still need testing.

## HUD temperatures

The FPS/frame-time HUD keeps one line and appends the available CPU, GPU
and battery temperatures. The hottest named CPU zone is shown as CPU;
`soc-thermal` is labeled SOC only when a CPU zone is unavailable. GPU and
battery readings are never substituted for CPU temperatures.

Thermal-zone values are converted from millidegrees Celsius; Android's
sticky battery intent is converted from tenths. Invalid or inaccessible
readings are omitted. If no sensor is readable the HUD shows `TEMP --`.
Sysfs discovery and polling run on a background worker once every two
seconds and stop when the activity pauses. No thermal files are read on the
render/UI thread; no new Android permissions or root are required.

The Vita firmware names the relevant zones `bigcore-thermal`,
`little-core-thermal`, `gpu-thermal`, `soc-thermal`, and `cw221X-bat`.
App access was verified independently from ADB: the installed, non-debuggable
build reported `CPU 43°C GPU 42°C BAT 31°C` without root. Readings change with
the load; these numbers are one sample, not a maximum or a safe-temperature
threshold. Unit/classification/fallback tests are included in the 28 passing
JVM test cases.

## Scope

All previous RG477M graphics fixes, the supplied artwork, 120 FPS upper
bound and fork update feed remain. Native sources under FEX are unchanged.
The test build uses the same application ID/signing certificate for in-place
installation, and the release label `hakuX RG Vita Pro` is selected by the
`hakuxAppName` Gradle property. Other builds keep the existing label.

## Published benchmark

See the [English test report](../docs/RG-VITA-PRO-PERFORMANCE.en.md) or
[Russian report](../docs/RG-VITA-PRO-PERFORMANCE.ru.md) for seven Most Wanted
configurations, the mixed five-minute warm-up, temperature limits and
recommended System-driver profiles. These results do not establish a
broad Vita Pro compatibility list.
