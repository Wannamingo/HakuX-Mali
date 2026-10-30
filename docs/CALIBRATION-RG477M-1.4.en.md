# RG477M calibration verification — 1.4/code22

[Русский](CALIBRATION-RG477M-1.4.ru.md) · [Usage](MEASURED-CALIBRATION.en.md)

October 30, 2026. HakuX-Mali 1.4/code22 test build, code `10d0a64fadc939fcac2c187ec8c93e26e4c3f7f9`. ANBERNIC RG477M, MT6897/Dimensity 8300, Mali-G615 MC6, GammaOS Full / Android 14, bundled MaliSX2 0.2.3. This build has not been published to GitHub.

## Conditions and results

Syberia, the hotel fireplace scene from our Slot 2. Every pass started from the same separate temporary checkpoint in a fresh native process. Each candidate warmed up for 10 seconds; the first measurement lasted 15 seconds and subsequent measurements 10 seconds. FXAA was off, with a common 120 FPS ceiling and no game-control input during measurements.

| Candidate | Measured seconds | FPS | P95 ms | CPU % | Peak RSS MiB |
|---|---:|---:|---:|---:|---:|
| 1×, Bilinear | 15.063 | 59.95 | 16.80 | 200.3 | 368.1 |
| 2×, Bilinear | 10.048 | 47.67 | 33.44 | 193.4 | 407.4 |
| 1×, GSR | 10.038 | 59.97 | 16.90 | 197.4 | 366.2 |
| 1×, Bilinear, repeated reference | 10.044 | 60.04 | 16.88 | 198.2 | 365.4 |

The algorithm selected **1×, Bilinear, FXAA off, 120 FPS limit**. FPS and P95 were close to GSR, so lower graphics load won the tie. First/reference FPS differed by approximately 0.15%, within the consistency threshold. Higher resolution reduced performance to approximately 48 FPS in this scene; a 120 FPS ceiling does not mean the game reaches 120 FPS.

CPU is summed process time: 100% equals one fully occupied core. Available sensor peaks across individual warmup/measurement passes were CPU/SoC 93.0–98.7°C, GPU 48.8–51.5°C, battery 29–30°C. These are short comparisons including post-load activity, not prolonged thermal tests. GPU timing is unavailable because the existing G615 `NO_TIMESTAMPS` workaround remains enabled. GPU temperature does not measure utilization.

## Behavior verified

- The measured profile was stored, displayed in game settings and reused on a normal Slot 2 load: actual 640×480 rendering and a 120 FPS ceiling.
- Turning Auto off through the interface restored the original manual 2× configuration: 1280×960 rendering and a 60 FPS ceiling. Manual values were not overwritten.
- Repeat calibration works from settings. Home keeps the app in the background; returning rejects the interrupted measurement and leaves the previous measured profile byte-identical.
- Cancellation and final checkpoint restoration worked. Temporary checkpoints and cache blobs were cleaned after successful restoration; no pending session remains.
- All 12 existing state files and original settings, HUD and controller profiles matched their pre-install checksums. User Slot 3 was not used. The sole intentional new preference enables measured Auto for Syberia; global settings are unchanged.
- All 64 unit tests in 17 suites passed, together with native compilation, `assembleRelease`, R8 and `lintVitalRelease`. Previous signing identity/package, unchanged driver/icon assets, and 16 KiB ZIP/ELF alignment were verified. Temporary diagnostic properties are empty.

APK: `HakuX-Mali-1.4-code22-release.apk`.

SHA-256: `f5e63bb28d5f277e998797b8ba0afae4c425deace5d214cd8941aaf061038a95`.

Results apply to this scene and the listed configurations. RG Vita Pro calibration and other games were not tested in this run. Syberia was left paused with measured Auto enabled for this game.
