# Serious Sam: startup hang in a package with an extra launcher

[Русский](SERIOUS-SAM.ru.md) | [English](SERIOUS-SAM.en.md)

A folder-based Serious Sam package marked **T-Rus Ryudo** was checked on RG477M. Its `default.xbe` opens an added menu; English Version starts `game.xbe`, which is supposed to start the actual game, `eng.xbe`. The intermediate launcher/trainer hangs before rendering begins. The existing Serious Sam graphics fixes remain in HakuX-Mali.

## Workaround

Use this only if `default.xbe`, `game.xbe` and `eng.xbe` are present in the **same game folder**. These instructions do not apply to an ISO or a different layout.

1. Fully close the game in the emulator.
2. Open the Serious Sam folder in a file manager. Back up the original `default.xbe` by renaming it, for example, to `default.xbe.hakux-launcher.bak`. If that backup already exists, use another name without overwriting it.
3. **Copy** `eng.xbe` within the same folder and name the copy `default.xbe`. Keep the original `eng.xbe`.
4. Leave `Main.gro`, `Movies*.gro` and all other game resources unchanged.
5. Refresh the HakuX-Mali library and launch the folder normally. The added menu and trainer are bypassed.

To undo the change, close the game, remove the copied `default.xbe` and restore the backup's original filename. This workaround does not modify game saves.

## Validation and limits

Direct startup of `eng.xbe` reached gameplay on the checked copy on both RG477M and RG Rotate (2026-10-30). This bypasses an incompatible launcher; it is not an automatic fix for every Russian-language release. The emulator does not rename game files automatically.

`eng.xbe` is the English executable. **On 2026-10-30, the owner confirmed that Russian voice audio was preserved on RG Rotate after this workaround in the same T-Rus Ryudo package.** Russian text was not separately verified; the outcome depends on the package’s resources. Use a compatible copy with the desired language resources where needed; this workaround does not promise localization repair. No game executables or data are included in the HakuX-Mali release.

Technical detail: the checked intermediate `game.xbe` accessed `0xC0200088`, causing guest exception `0xC0000005`. The actual game's Title ID is `54540004`; this package's added launchers use `00000005` and `00000000`. Other layouts or failures need separate diagnosis.
