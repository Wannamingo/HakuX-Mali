package com.rfandango.haku_x.ui.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.*
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.Section

@Composable
internal fun PictureSettingsSection(app: AppPrefs, game: Boolean = false,
  values: Map<GameSetting, String?> = emptyMap(), onOverrideChange: (GameSetting, String?) -> Unit = { _, _ -> }) {
  val context = LocalContext.current
  val globals = remember { mutableStateMapOf<PictureControl, Int>().apply {
    PictureControl.entries.forEach { put(it, app.picture(it)) }
  } }
  Section(stringResource(R.string.extra_picture))
  Hint(stringResource(R.string.extra_picture_desc))
  TextButton(onClick = {
    for (control in PictureControl.entries) {
      if (game) onOverrideChange(control.setting, null) else {
        app.setPicture(control, 100); globals[control] = 100
      }
    }
  }) { Text(stringResource(if (game) R.string.extra_picture_global else R.string.extra_picture_reset)) }
  for (control in PictureControl.entries) {
    val own = values[control.setting]?.toIntOrNull()
    val effective = control.sanitize(if (game && own != null) own else globals.getValue(control))
    val note = listOfNotNull(
      if (control == PictureControl.SHARPNESS) stringResource(R.string.extra_sharpness_desc) else null,
      if (game) stringResource(if (own == null) R.string.extra_picture_inherited else R.string.extra_picture_override) else null,
    ).joinToString(" ").ifEmpty { null }
    SettingsTarget(pictureLabel(control)) {
      IntSettingSlider(title = stringResource(pictureLabel(control)), value = effective,
        range = control.minimum..control.maximum, step = 5,
        label = { context.getString(R.string.extra_percent, it) },
        note = note,
        onChange = {
          if (game) onOverrideChange(control.setting, it.toString()) else {
            app.setPicture(control, it); globals[control] = it
          }
        })
    }
    if (game && own != null) TextButton(onClick = { onOverrideChange(control.setting, null) }) {
      Text(stringResource(R.string.extra_picture_use_global_value))
    }
  }
}

internal fun pictureLabel(control: PictureControl) = when (control) {
  PictureControl.BRIGHTNESS -> R.string.extra_brightness
  PictureControl.CONTRAST -> R.string.extra_contrast
  PictureControl.SATURATION -> R.string.extra_saturation
  PictureControl.GAMMA -> R.string.extra_gamma
  PictureControl.SHARPNESS -> R.string.extra_sharpness
}
