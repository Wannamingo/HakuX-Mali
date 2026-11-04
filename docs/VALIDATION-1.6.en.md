# HakuX-Mali 1.6 / code26 validation

[Русский](VALIDATION-1.6.ru.md) | [English](VALIDATION-1.6.en.md)

Release APK: `HakuX-Mali-1.6-release.apk`.
SHA-256: `34e61e70a7513ff3ed912d4898396befdfebd88fcd04904f80529f69a1cce5f8`.
The existing signing identity is retained. The APK is non-debuggable and ARM64; APK/native-library 16 KiB alignment was checked.

| Check | Result |
|---|---|
| Final Android APK | 101 unit tests, release lint, build and signature verification passed |
| Scheduler and file I/O | 2841 CPU checks on RG477M, using production code with mocked guest memory and real Android temporary files |
| Native core | Full NDK 29 ARM64 link; the final APK retains the exact validated binary |
| FSR 1 | Beyond Good & Evil introductory movie, 640×480 → 1280×960; EASU + RCAS confirmed |
| Exact Unified P48r1 | 149 CTS cases and 24 native processes: 34,746 checks and 314 explicitly counted GPU cases |
| P48r1 app loading | Syberia intro and main menu on RG476H and RG477M, with actual runtime driverInfo verified |
| In-place update | 1.6/code26 installed over previous builds on RG477M and RG476H |

On RG477M, the final APK with bundled P48r1 cold-booted Syberia and reached the first hotel gameplay scene. The Russian SPEED 100% label, changing metrics, unclipped single-line layout, pause and resume were verified. Original M2+DS1, seven small settings, the SharedPreferences file list, 20 state files, seven debug properties and locale were restored and verified. An old state from a different XBE copy was rejected before section loading and left unchanged. [Final capture](screenshots/1.6/HakuX-Mali-1.6-RG477M-HUD-Speed.png).

On RG476H, the final APK with bundled P48r1 cold-launched Syberia, displayed the introductory logos/movie and reached the main menu. Actual driverInfo was verified; no GPU faults or native crashes were observed during this window. The previous Unified P37 was restored, with APK identity and debug properties preserved. Stock firmware does not expose private app files for byte-level verification. [Final capture](screenshots/1.6/HakuX-Mali-1.6-RG476H-Syberia.png).

Native core: `82b2539727c66efb9b38b31d0f6cb88c1ed9ab320e4c93673e45d0b53c6eb893`.
P48r1 library: `71e520f9de773d23700b2a5e48e6960dbd588d791b3e6b0ef31f77b5d17330df`.
FEX remains at `52018bf46a8279b22bce738548ab3f40db467064`.

RG477M restoration is verified by exact selected-driver, small-preference, controller and HUD hashes. Stock RG476H does not expose private files for those hashes; APK identity, observable settings and the actual selected driver are checked. Application data is retained; user HDD/state slots are not replaced.

These checks do not establish full playthroughs, FPS superiority or full Vulkan conformance. The [74 folders confirmed by the owner on RG477M](COMPATIBILITY.en.md) belong to the separate November 2 check. Every new feature was not rechecked on Vita Pro and Rotate; earlier 1.4 validation retains its original scope.

The source archive contains the corresponding app, FEX and dependency sources. Separate licensed source attachments accompany the retained M2+DS1 and new P48r1 bundles. No BIOS, games, user EEPROM/HDD/state files or signing key are included.
