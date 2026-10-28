# HakuX for RG477M

[Русский](README.md) | [English](README.en.md)

<img src="android/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="128" alt="HakuX for RG477M icon">

A free, unofficial [hakuX](https://github.com/rfandango/hakuX) adaptation for **ANBERNIC RG477M / Mali-G615 MC6**, Android arm64 and **MaliSX2 0.2.3**. The current release is based on hakuX 1.2.0. Upstream credits and component licenses are preserved.

**1.2.0-rg477m.2**: bundled MaliSX2, Dimensity 8300 detection, fork updates and a verified 4:3/16:9 switch. Installed and tested on RG477M. [Release notes](releases/1.2.0-rg477m.2.en.md).

## Download

**[1.2.0-rg477m.2 — APK, sources and checksums](https://github.com/Wannamingo/hakuX-for-RG477M/releases/tag/v1.2.0-rg477m.2)**

The published APK is identical to the version installed and tested on RG477M. It is an optimized, non-debuggable arm64-v8a release build. The `com.rfandango.haku_x.debug` package ID is retained to update our earlier installation; the suffix does not enable debugging.

## Install

1. Install the APK from Releases. Updating our earlier build retains data and settings.
2. On a fresh installation, complete setup and select your own Xbox ISO/XISO folder.
3. MaliSX2 0.2.3 is bundled and selected automatically on a fresh RG477M / supported MT6897 installation. Updates preserve an explicit system/other driver choice; the built-in package is available in Settings → GPU driver. Restart the game after changing drivers.
4. The fresh-install default is **120 FPS**; existing saved values are retained. Change it in Settings → Frame rate limit. Games with an internal 30/60 FPS cap retain it.
5. When enabled, the FPS HUD shows only FPS and average/last frame time.

The built-in update check now uses Releases from **Wannamingo/hakuX-for-RG477M**. APKs must match the installed package ID and signing certificate; Android asks before installation. Version .1 still queried upstream: install .2 manually from this release first.

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
- The release HUD has one row for FPS and average/last frame time. Lower diagnostic rows and temporary experimental modes are absent.
- The supplied artwork is used for legacy, adaptive and monochrome icons across five densities, preserving its aspect ratio.
- Updated to hakuX 1.2.0, retaining upstream audio, memory, thread-stack, controller and System Link changes. present_cpu_wait and rop_pass_split have distinct bits; fpslimit uses the shared parser, and the adrenotools hooks are built together.

See the complete [release notes](releases/1.2.0-rg477m.2.en.md) and [technical details](android/RG477M.md).

## Successfully launched games

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

The Call of Cthulhu introductory cinematic texture defect remains unresolved. Every Ultimate Spider-Man comic scene and long gameplay sessions have not been checked. Conker: Live & Reloaded is not yet included in confirmed gameplay checks. Dimensity 8300/8300 Ultra is recognized through MT6897 identifiers, but actual hardware testing was on RG477M.

## Building and sources

See [android/README.md](android/README.md) and [android/RG477M.md](android/RG477M.md). GitHub’s regular source ZIP does not include submodule contents; the release includes a separate source archive with FEX, the native dependencies used, licenses and a revision manifest. The required upstream 1.2.0 adrenotools patch is included.

## License and credits

hakuX is free software under **GPLv3 or later**: [COPYING](COPYING), [LICENSE](LICENSE), [licenses/](licenses/). Upstream: [rfandango/hakuX](https://github.com/rfandango/hakuX); its FEX fork and xemu-based APU retain their credits and licenses. This fork is not affiliated with Microsoft or ANBERNIC. Games, BIOS, HDD images, user data and private signing keys are not distributed.
