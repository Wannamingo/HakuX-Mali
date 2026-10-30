package com.rfandango.haku_x.data

/** Percentages; 100 preserves the existing picture, including SGSR's current edge strength. */
enum class PictureControl(val setting: GameSetting, val minimum: Int, val maximum: Int) {
  BRIGHTNESS(GameSetting.BRIGHTNESS, 50, 150),
  CONTRAST(GameSetting.CONTRAST, 50, 150),
  SATURATION(GameSetting.SATURATION, 0, 200),
  GAMMA(GameSetting.GAMMA, 50, 200),
  SHARPNESS(GameSetting.SHARPNESS, 0, 100);

  fun sanitize(value: Int) = value.coerceIn(minimum, maximum)
}
