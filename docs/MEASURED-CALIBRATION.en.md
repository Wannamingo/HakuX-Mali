# Measured performance calibration — 1.4/code22

[Русский](MEASURED-CALIBRATION.ru.md)

Choose **Calibrate performance** from the pause menu in a stable gameplay scene. The emulator creates a separate temporary checkpoint and compares several graphics configurations from it. Apply the result for this game; Auto uses the measured profile, while switching Auto off restores manual graphics settings on the next launch.

## Comparison

Candidates are 1×/bilinear/FXAA off, the current graphics configuration, 2×/bilinear/FXAA off, and 1×/GSR/FXAA off. Duplicates are removed, then the first reference is repeated to detect changed conditions. All candidates use the same display-based FPS ceiling, up to 120; system power saving uses the current display rate. Each candidate runs in a fresh native process from the same checkpoint, waits for loading and shader prebuild, then warms up for 10 seconds. The first measurement lasts 15 seconds, all subsequent measurements last 10 seconds. Game controls are blocked; Cancel or Back/B restores the checkpoint.

The winner prioritizes measured FPS. Candidates within 3% of the best are compared by P95 frame interval, then lower graphics load. A higher resolution can win if it is measurably faster; 1× is not a predetermined result. Reference drift above 10% in FPS or 25% in P95 prevents publishing a new profile. Existing profiles are retained.

## Metrics and limits

FPS and frame intervals come from Xbox front-image changes inside the core, rather than HUD polling or panel refreshes. They do not measure button-to-screen input latency. Storage is bounded to 8192 intervals, without pixel histories or additional GPU waits. Measurements include FPS, median/P95/maximum frame interval, whole-process CPU time, process peak RSS, and available temperature peaks during warmup and measurement. CPU 100% equals one fully occupied core; totals can exceed 100%. RSS includes loading the fresh process.

Optional GPU timestamps are independent of the debug profiler, read without waiting, and respect existing driver workarounds and queue timestamp capabilities. No data is reported as unavailable. The metric is summed time of measured command buffers and their count, not GPU utilization. The existing Mali-G615 `NO_TIMESTAMPS` workaround remains enabled. Its comment attributes the issue to ARMSX2; timestamp-query hangs have not been independently reproduced on our RG477M.

The result applies to this scene and the compared configurations. It does not establish the best settings for every level or replace prolonged thermal testing. Keep the system power mode and cooling unchanged. A severe Android thermal status or the configured battery-temperature threshold stops comparison. No universal CPU temperature cutoff is assumed.

## State and recovery

Calibration requires core savestate support. Temporary files live under `files/performance/sessions/<UUID>/`, separate from manual slots 1–10, automatic slot 0 and quick slot 11. Every pass and final return uses that checkpoint. A crash, background interruption or cancellation never counts as a successful measurement. Graphics settings offer recovery of an unfinished comparison. The checkpoint and its cache blobs are removed only after the final restored game shows a frame.

**Apply result and enable Auto for this game** stores the measured profile and enables Auto for this game. **Restore checkpoint with previous settings** retains the old profile and restores the previous graphics configuration. Profiles are checked against the game, system build/SoC, selected driver and emulator version. A changed device, driver or version requires new measurements. Auto without a matching profile uses clearly labelled initial low-load settings and offers calibration.

This compares graphics configurations; it does not change CPU/GPU clocks, aspect ratio, audio, compatibility fixes or drivers. Dynamic resolution, anisotropic filtering and automatic movie aspect detection remain separate work.
