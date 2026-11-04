# Android CPU regression harness

These tests exercise the production dispatcher, scheduler, completion ports and
file-sharing implementation with a mocked guest-memory/backend interface. They
do not run Xbox games or submit GPU work. The file-sharing checks create and
remove their own temporary directory under `/data/local/tmp`; they do not use
the emulator HDD or game folder.

From the repository root, using the Android NDK 29 Clang for your host:

```sh
<ndk>/toolchains/llvm/prebuilt/<host>/bin/clang \
  --target=aarch64-linux-android28 -O1 -g -std=gnu11 \
  -ffunction-sections -fdata-sections -I. \
  tests/native/xbased/kernel.c tests/native/xbased/sharing.c native/kernel/ps.c \
  -Wl,--gc-sections -Wl,-z,max-page-size=16384 -lm -pthread \
  -o build/xbased-cpu-tests
adb push build/xbased-cpu-tests /data/local/tmp/hakux-xbased-cpu-tests
adb shell chmod 755 /data/local/tmp/hakux-xbased-cpu-tests
adb shell /system/bin/timeout 30 /data/local/tmp/hakux-xbased-cpu-tests
```

Create `build/` first. On Windows use `windows-x86_64/bin/clang.exe` and your
shell's line-continuation syntax. Select the intended device when several are
connected. Successful output reports `PASS xbased Android ARM64 CPU-only` and
the check count. The release integration run on RG477M reported 2841 checks.

Coverage includes priority boosts and decay, yield/waits, completion queue
growth beyond 512 packets, state round trips and malformed-state rejection,
and sharing conflicts checked before truncation, with file contents preserved.
This harness does not establish complete game compatibility or FPS gains.

Android UI/controller tests are run separately with
`testReleaseUnitTest`; release lint uses `lintVitalRelease`.
