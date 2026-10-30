# ANBERNIC RG VITA Pro: hakuX test results

[Русский](RG-VITA-PRO-PERFORMANCE.ru.md) | [English](RG-VITA-PRO-PERFORMANCE.en.md)

Date: **2026-10-29**. APK **1.2.0-rg477m.2-vita.2**, versionCode **12**, executable-source snapshot `6c460c8cb1c4eb099784f9df3a28085f93a81dee`. RG VITA Pro: **Rockchip RK3576 / Mali-G52 / Android 14 / GammaOS Next Lite**, kernel 6.1.141, approximately 4 GB RAM.

## Test coverage

Seven configurations used the same Slot 1 of **Need for Speed: Most Wanted - Black Edition**, Xbox title ID `4541007B`, game version 7. Each short measurement ran for approximately 60 seconds after resuming the race. A separate five-minute run used 2x.

| Configuration | Mean FPS | 5-second FPS range | CPU / GPU maximum °C |
|---|---:|---:|---:|
| MaliSX2 0.2.3, 1x | 30.0 | 30.0-30.0 | - / - |
| MaliSX2 0.2.3, 2x | 21.4 | 20.2-22.1 | 66.5 / 67.5 |
| System, 1x | 30.0 | 30.0-30.0 | 60.1 / 60.1 |
| System, 1x + FXAA | 30.0 | 30.0-30.0 | 66.5 / 65.6 |
| System, 2x | 29.9 | 29.7-30.0 | 65.6 / 67.5 |
| System, 3x | 18.1 | 15.9-19.6 | 68.4 / 71.2 |
| System, 4x | 10.1 | 9.2-13.1 | 69.3 / 72.1 |
| System, 2x, 5 min* | 30.0 | 29.9-30.0 | 71.2 / 73.0 |

Mean FPS uses presented-frame counts and interval durations from `hakuX-hle-fps`, excluding the first two intervals after resuming. The range is the minimum and maximum of five-second windows, **not 1% low**. The first MaliSX2 1x run's complete temperature series was not retained; missing values are not replaced with pause-menu readings.

\* Five minutes include the active race scene and the standings screen after elimination. The first **120 seconds** are verified as the active race scene, averaging **30.0 FPS**. Elimination is visible at 142 s and standings by 160 s. This is not five minutes of uninterrupted racing. Whole-run sampled maxima: **CPU 71.2 °C / GPU 73.0 °C / battery 36.0 °C**.

## Recommended profiles for this scene

| Profile | Settings | Result |
|---|---|---|
| Minimum load | System driver, 1x, FXAA Off, Bilinear | 30 FPS; starting point for longer sessions |
| Balanced | System driver, 1x, FXAA On, Bilinear | 30 FPS with anti-aliasing in the one-minute test |
| Highest practical clarity | System driver, 2x, FXAA Off, Bilinear | 29.9 FPS in the one-minute race test, more heat |

3x and 4x are not recommended for this scene: approximately 18 and 10 FPS. At the end of 4x, Thermal HAL additionally reported GPU 73.9 °C with `mStatus=3 (SEVERE)`. Clocks were not recorded; the amount of clock throttling is not established. Use 1x for less heat. The device was left on System driver with a per-game 1x / FXAA Off override for Most Wanted; other games' global resolution was retained.

All runs used 16:9, asynchronous shader compilation On, cached-pipeline prebuild On, Bilinear, Uncapped frame rate Off and a **120 FPS** upper bound. That bound does not remove a game's own frame-rate limit. GPU driver selection is global; resolution changes used per-game settings. Restart the game after changing the driver. Internal 1x/2x/3x/4x frame sizes were 640x480 / 1280x960 / 1920x1440 / 2560x1920, presented on 1920x1080. This benchmark does not add a new Vita Pro 4:3 versus 16:9 comparison.

## Method and limitations

- Reload the state, resume the race, and hold the on-screen accelerator without steering. The car spends part of the run against the barrier. No complete lap or circuit was driven. The state slot was not overwritten.
- Sample temperatures approximately every 5 s: the maximum of `bigcore-thermal` / `little-core-thermal`, plus `gpu-thermal` and `cw221X-bat`. These are sampled maxima, not continuous peaks. The HUD itself updates every 2 s.
- Tests were sequential on a warm device charging over USB. Ambient temperature, brightness and fixed clocks were not controlled; firmware thermal limits were not disabled. Temperatures cannot be compared directly as driver efficiency. Battery life and 30-60 minute sessions were not measured.
- Earlier, DOA Xtreme Beach Volleyball reached its intro/title at approximately 60 FPS; on-screen Start and the temperature HUD were checked. This is **not an in-level gameplay benchmark**.
- The 31 confirmed startup entries belong to RG477M and **do not automatically establish Vita Pro compatibility**. Other games and firmware need separate checks.

## Original GPU failure

One Most Wanted 2x run on MaliSX2 reported `0x60 (out of memory)` for a vertex/tiler/compute atom, followed by `VK_ERROR_DEVICE_LOST` in `vkWaitForFences`. This is memory exhaustion while executing a GPU job, not proof that all system RAM was exhausted. At that failure, the HUD showed approximately CPU 51 / GPU 50 / battery 32 °C; attributing that failure to overheating is not supported.

The failure did not recur in the controlled reruns, but **the MaliSX2 defect is not claimed fixed**. A practical starting configuration for Most Wanted is System driver and 1x; try 2x afterwards if desired.

Data: [CSV](rg-vita-pro-performance.csv), [JSON](rg-vita-pro-performance.json). [PDF with charts, Russian](https://github.com/Wannamingo/HakuX-Mali/releases/download/v1.2.0-rg477m.2/hakuX-RG-Vita-Pro-performance-2026-10-29.pdf). [Build notes](../releases/1.2.0-rg477m.2-vita.2.en.md).
