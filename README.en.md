# HakuX-Mali

[Русский](README.md) | [English](README.en.md)

![HakuX-Mali](assets/branding/hakux-mali-banner.png)

An Android Mali fork of the Xbox emulator [hakuX](https://github.com/rfandango/hakuX). **One 1.4/code22 release supports ANBERNIC RG477M, RG Vita Pro and RG Rotate.** Based on hakuX 1.2.0.

**[Release 1.4: APK, source and checksums](https://github.com/Wannamingo/HakuX-Mali/releases/tag/v1.4)** · **[Detailed changes](releases/1.4.en.md)**

## New in 1.4

- Measured calibration: several configurations from one temporary checkpoint, actual FPS/P95, game-specific results and cancellation/crash recovery.
- Automatic settings that preserve manual values, plus a 30 FPS Economy preset.
- Game volume, mute, three audio-buffer choices and HRTF.
- Edge cropping, zoom, positioning and display refresh selection; retained 4:3 / 16:9.
- Optional normal-exit autosaves, resume prompts, a separate quick slot and Select + button shortcuts.
- Stick sensitivity/inversion, rumble strength, settings export/import.
- Temperature warnings and the retained single-row FPS/frametime/temperature HUD.
- Extended Syberia menu-background fix for imported shared RAM on Rotate.

## Supported handhelds

| Handheld | Platform | Starting settings |
|---|---|---|
| RG477M | Dimensity 8300 / Mali-G615 | Bundled MaliSX2 0.2.3, 1×, Bilinear, FXAA Off |
| RG Vita Pro | RK3576 / Mali-G52, Android 14 / GammaOS Next Lite | System driver, 1×, Bilinear, FXAA Off |
| RG Rotate | T618 / Mali-G52, Android 12 | System driver / Vulkan 1.1, 1×, Bilinear, FXAA Off |

A full 1.4 calibration cycle on RG477M measured about 60 FPS at 1× versus 48 at 2× in one Syberia scene. The 1.4 line reached Syberia gameplay on Rotate; the final APK passed Syberia menu checks and reached a Serious Sam level, around 60 FPS in captured frames. Vita Pro received 1.4 with verified data preservation before the Rotate extension; its Most Wanted benchmarks are from an earlier build: System 1× 30 FPS, System 2× 29.9 FPS. These checks are not one comparable cross-device benchmark.

[Calibration method](docs/MEASURED-CALIBRATION.en.md) · [RG477M results](docs/CALIBRATION-RG477M-1.4.en.md) · [Vita Pro benchmarks/temperatures](docs/RG-VITA-PRO-PERFORMANCE.en.md) · [1.4 gallery](docs/screenshots/1.4/README.md)

## Installation

1. Install **HakuX-Mali-1.4-release.apk** over our previous release. Package ID and signing identity are retained; do not uninstall to update.
2. Select your own ISO/XISO directory or game folders containing `default.xbe`. Release source archives are for developers.
3. MaliSX2 is bundled for RG477M; an explicit existing driver choice is retained on update. Rotate was checked with its system driver. Restart games after switching drivers.
4. Auto is off by default. Use a stable gameplay scene and **Calibrate performance** in the pause menu to measure settings.
5. A new installation defaults to a 120 FPS ceiling; existing manual limits are preserved. Games retain their own lower 30/60 FPS caps.

Built-in updates use **Wannamingo/HakuX-Mali**. ES-DE: Microsoft Xbox → HakuX-Mali (Standalone), [configurations](https://github.com/Wannamingo/es-de-android-custom-systems). First grant the emulator access to the same games directory.

## Serious Sam and Russian voice audio

The T-Rus Ryudo folder package has a [launcher workaround](docs/SERIOUS-SAM.en.md): back up the original `default.xbe`, then copy `eng.xbe` as `default.xbe`. It reached gameplay on RG Rotate; **the owner confirmed preserved Russian voice audio**. Russian text was not separately verified. The emulator does not automatically rename game files.

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

See the complete [release notes](releases/1.4.en.md) and [technical details](android/RG477M.md).

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
