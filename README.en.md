# HakuX-Mali

[Русский](README.md) | [English](README.en.md)

<img src="assets/branding/hakux-mali-banner.png" width="100%" alt="HakuX-Mali — Console emulation, higher possibilities">

A free, unofficial [hakuX](https://github.com/rfandango/hakuX) adaptation for **ANBERNIC RG477M / Mali-G615 MC6**, with experimental **ANBERNIC RG VITA Pro / RK3576 / Mali-G52** support, Android arm64 and **MaliSX2 0.2.3**. The current release is based on hakuX 1.2.0. Upstream credits and component licenses are preserved.

**1.2**: refreshed library and menus, categorized settings and search, Russian/English UI, graphics profiles, picture and HUD controls, per-game FPS overrides, and Syberia fixes. **[Detailed release notes](releases/1.2.en.md) · [Menu and settings screenshots](docs/screenshots/1.2/README.md) · [Serious Sam launcher workaround](docs/SERIOUS-SAM.en.md).**

## Menus and settings in 1.2

Global settings have six categories; individual games have overrides with visible inheritance. Russian/English search jumps to the matching row. Application settings choose the interface language independently of the Xbox language. Graphics provides Performance / Balanced / Quality profiles, brightness, contrast, saturation, gamma and GSR strength. Profiles and picture changes apply on the next game launch. GSR is an upscaling filter; MaliSX2 is the Vulkan driver.

The single-line HUD now has font, corner, background and independent CPU/SoC, GPU and battery controls. Fresh installations retain a 120 FPS limit; existing choices are preserved. **[Screenshots of the main screens](docs/screenshots/1.2/README.md).**

![HakuX-Mali 1.2 library](docs/screenshots/1.2/01-library.png)

![Graphics settings](docs/screenshots/1.2/03-graphics.png)

Syberia fixes address the checked 3D flicker, trails and menu background refresh. Certain Russian **Serious Sam** folder packages with an added menu/trainer have a **[separate workaround](docs/SERIOUS-SAM.en.md)**. It bypasses the launcher using `eng.xbe`; preservation of Russian localization was not confirmed.

For ES-DE, use **Microsoft Xbox → HakuX-Mali (Standalone)** in [our configuration repository](https://github.com/Wannamingo/es-de-android-custom-systems). First grant the emulator access to the same games folder. See the release notes for details.

## ANBERNIC RG VITA Pro

Retained **experimental RG VITA Pro / RK3576 / Mali-G52 support** from **1.2.0-rg477m.2-vita.2**: system Vulkan 1.3 startup, bundled MaliSX2, and CPU/GPU/battery temperatures in the single-line HUD. Tested on Android 14 / GammaOS Next Lite.

- [Current HakuX-Mali 1.2 APK](https://github.com/Wannamingo/HakuX-Mali/releases/download/v1.2/HakuX-Mali-1.2-release.apk) · [Earlier Vita Pro validation](releases/1.2.0-rg477m.2-vita.2.en.md) · [Tests and recommended settings](docs/RG-VITA-PRO-PERFORMANCE.en.md).
- Most Wanted, one saved race scene: System 1x - **30 FPS**, System 1x + FXAA - **30 FPS**, System 2x - **29.9 FPS**, MaliSX2 2x - **21.4 FPS**. Start with **System driver / 1x**. 3x/4x yielded approximately 18/10 FPS.
- The older 31-startup list belongs to RG477M; compatibility of that entire list on Vita Pro is not claimed. The five-minute run includes standings after elimination; the report explains the stages and temperatures.

## Download

**[HakuX-Mali 1.2 — APK, sources and checksums](https://github.com/Wannamingo/HakuX-Mali/releases/tag/v1.2)**

A single Android arm64 APK, `HakuX-Mali-1.2-release.apk`, retains support for both handhelds. Version 1.2 was installed on RG477M with data-preservation checks. Vita Pro measurements belong to an earlier build; no new 1.2 benchmark was performed on that console. This is an optimized, non-debuggable release. The `com.rfandango.haku_x.debug` package ID and existing signing certificate are retained for in-place updates with data preserved.

## Install

1. Install `HakuX-Mali-1.2-release.apk` over our earlier build to retain data and settings.
2. On a fresh installation, complete setup and select the directory containing your own Xbox ISO/XISO images or extracted game folders. Each game folder must contain `default.xbe` and the remaining game files.
3. MaliSX2 0.2.3 is bundled and selected automatically on a fresh RG477M / supported MT6897 installation. Updates preserve an explicit system/other driver choice; the built-in package is available in Settings → Graphics → GPU driver. Restart the game after changing drivers.
4. The fresh-install default is **120 FPS**; existing saved values are retained. Change it in Settings → Display & HUD → Frame rate limit. Games with an internal 30/60 FPS cap retain it.
5. The single-line HUD shows FPS, average/last frame time and available CPU/GPU/battery temperatures.

The built-in update check uses Releases from **Wannamingo/HakuX-Mali**. Install **1.2 manually once** when migrating from the old name: earlier builds use the old address and a different version scheme. Future HakuX-Mali updates are checked in the app; package ID and signature checks remain enabled.

## RG477M fixes

- MaliSX2 import/loading on RG477M, actual driver verification and explicit load errors.
- A bounded 64-fence completion queue and compatible presentation for drivers without timeline semaphores.
- Syberia and Ultimate Beach Soccer: fixed early exit from file-backed images by handling the in-memory XBE media flags.
- Serious Sam: guest SEH delivery and REP MOVS progress recovery fix the startup crash.
- Ultimate Spider-Man: PAGE_WRITECOMBINE attributes are preserved using the expanded upstream 1.2.0 memory fix, including MmSetAddressProtect and compatible MM v2 state fields.
- Serious Sam: unavailable/incomplete visibility queries no longer become false zero results, fixing black flicker and severe input delay.
- Warrior Within: linear video textures are matched by pitch, texel size and dimensions, fixing stripes and the stretched lower half of cutscenes.
- The Two Thrones: unaligned vertex arrays are repacked without changing attribute values, fixing excess character geometry and triangles.
- The Sands of Time: exact bloom texture bounds remove the green right edge while retaining the effect.
- The Sands of Time / MaliSX2 0.2.3: splitting legacy render passes at blend/write-mask transitions and synchronizing attachments remove flickering black sky/floor blocks.
- FPS limits: unlimited, 30, 60, 90 or 120. The tested console retains 120 FPS; the limiter avoids a frame backlog and preserves a game’s own lower cap.
- The release HUD has one row for FPS, average/last frame time and available CPU/GPU/battery temperatures. Lower diagnostic rows and temporary experimental modes are absent.
- The supplied artwork is used for legacy, adaptive and monochrome icons across five densities, preserving its aspect ratio.
- Updated to hakuX 1.2.0, retaining upstream audio, memory, thread-stack, controller and System Link changes. present_cpu_wait and rop_pass_split have distinct bits; fpslimit uses the shared parser, and the adrenotools hooks are built together.

See the complete [release notes](releases/1.2.en.md) and [technical details](android/RG477M.md).

## Successfully launched games on RG477M

**31 confirmed startups**: the owner confirmed 29 images on 2026-10-27, Syberia was checked earlier, and Call of Cthulhu gameplay was confirmed on .2 on 2026-10-28. This does not establish complete playthroughs or defect-free graphics; the earlier 30 games were not all rechecked on .2. [Test scope and confirmation sources](docs/COMPATIBILITY.en.md).

- Call of Cthulhu: Dark Corners of the Earth — gameplay works; missing textures remain in its introductory cinematic.
- Colin McRae Rally 2005
- Dead or Alive Xtreme Beach Volleyball
- Jet Set Radio Future
- Manhunt
- Midnight Club 3: DUB Edition
- Need for Speed: Carbon
- Need for Speed: Hot Pursuit 2
- Need for Speed: Most Wanted – Black Edition
- Need for Speed: Underground 2
- OutRun 2006: Coast 2 Coast
- Pinball Hall of Fame: The Gottlieb Collection
- Prince of Persia: The Sands of Time
- Prince of Persia: The Two Thrones
- Prince of Persia: Warrior Within
- Pro Evolution Soccer 5
- Pro Tennis WTA Tour (Europe)
- Rayman 3: Hoodlum Havoc
- Serious Sam
- Serious Sam II
- Soulcalibur II
- Spyro: A Hero’s Tail
- SSX on Tour
- Street Racing Syndicate (SRS)
- Syberia
- Tennis Masters Series 2003
- Ultimate Beach Soccer
- Ultimate Pro Pinball
- Ultimate Spider-Man
- WTA Tour Tennis (USA)
- XGRA: Extreme G Racing Association

## Known limits

Checked on 2026-10-30: **Manhunt** starts directly from a folder with 969 files and reaches gameplay at approximately **50 FPS** in the PAL version. No full playthrough was performed.

The Call of Cthulhu introductory cinematic texture defect remains unresolved. Every Ultimate Spider-Man comic scene and long gameplay sessions have not been checked. Conker: Live & Reloaded is not yet included in confirmed gameplay checks. Dimensity 8300/8300 Ultra is recognized through MT6897 identifiers, but actual hardware testing was on RG477M.

## Building and sources

See [android/README.md](android/README.md) and [android/RG477M.md](android/RG477M.md). GitHub’s regular source ZIP does not include submodule contents; the release includes a separate source archive with FEX, the native dependencies used, licenses and a revision manifest. The required upstream 1.2.0 adrenotools patch is included.

## License and credits

hakuX is free software under **GPLv3 or later**: [COPYING](COPYING), [LICENSE](LICENSE), [licenses/](licenses/). Upstream: [rfandango/hakuX](https://github.com/rfandango/hakuX); its FEX fork and xemu-based APU retain their credits and licenses. This fork is not affiliated with Microsoft or ANBERNIC. Games, BIOS, HDD images, user data and private signing keys are not distributed.
