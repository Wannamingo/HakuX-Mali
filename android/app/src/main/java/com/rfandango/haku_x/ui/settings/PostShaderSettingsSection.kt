package com.rfandango.haku_x.ui.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.*
import com.rfandango.haku_x.ui.common.ChoiceDialog
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.Section

@Composable
internal fun ColumnScope.PostShaderSettingsSection(gameKey: String?) {
  val context = LocalContext.current
  val store = remember { ExperienceStore(context) }
  var revision by remember { mutableIntStateOf(0) }
  val options = remember(gameKey, revision) { store.effective(gameKey) }
  val own = remember(gameKey, revision) { store.own(gameKey) }
  var choosing by remember { mutableStateOf<ExperienceSetting?>(null) }
  fun update(values: Map<ExperienceSetting, Int?>) {
    runCatching { store.setAll(values, gameKey) }.onSuccess { revision++ }
      .onFailure { Toast.makeText(context, R.string.extra_save_failed, Toast.LENGTH_LONG).show() }
  }
  fun valueLabel(field: ExperienceSetting, raw: Int): String = when {
    field == ExperienceSetting.POST_SHADER -> context.getString(PostShaders.label(raw))
    raw == field.default -> context.getString(R.string.shader_from_preset)
    field == ExperienceSetting.POST_MASK -> context.getString(PostShaders.maskLabel(raw))
    else -> context.getString(R.string.shader_pixels, raw)
  }
  SettingsGroup(true) {
    Section(stringResource(R.string.shader_title))
    Hint(stringResource(R.string.shader_controls_desc))
    for (field in PostShaders.allFields) {
      val raw = options[field]
      val inherited = gameKey != null && field !in own
      val note = listOfNotNull(
        if (inherited) stringResource(R.string.exp_inherited) else null,
        if (field != ExperienceSetting.POST_SHADER && PostShaders.followsPreset(options, field))
          stringResource(R.string.shader_from_preset) else null,
      ).joinToString(" · ").ifBlank { null }
      SettingsTarget(PostShaders.title(field)) {
        if (field in listOf(ExperienceSetting.POST_SHADER, ExperienceSetting.POST_MASK, ExperienceSetting.POST_MASK_SIZE)) {
          val actual = if (field == ExperienceSetting.POST_SHADER) raw else PostShaders.value(options, field)
          val label = if (field == ExperienceSetting.POST_MASK) stringResource(PostShaders.maskLabel(actual))
            else if (field == ExperienceSetting.POST_MASK_SIZE) stringResource(R.string.shader_pixels, actual)
            else stringResource(PostShaders.label(actual))
          ListItem(headlineContent = { Text(stringResource(PostShaders.title(field))) },
            supportingContent = { Text(listOfNotNull(note, label).joinToString(" · ")) },
            modifier = Modifier.clickable { choosing = field })
        } else {
          IntSettingSlider(stringResource(PostShaders.title(field)), PostShaders.value(options, field),
            0..field.maximum, label = { context.getString(R.string.shader_percent, it) }, note = note,
            onChange = { update(mapOf(field to it)) })
        }
        Row {
          if (field != ExperienceSetting.POST_SHADER && raw != field.default) TextButton(onClick = {
            update(mapOf(field to field.default))
          }) { Text(stringResource(R.string.shader_from_preset)) }
          if (gameKey != null && field in own) TextButton(onClick = { update(mapOf(field to null)) }) {
            Text(stringResource(R.string.extra_picture_use_global_value))
          }
        }
      }
    }
    TextButton(onClick = { update(PostShaders.fields.associateWith { it.default }) }) {
      Text(stringResource(R.string.shader_reset_parameters))
    }
  }
  choosing?.let { field ->
    val values = when (field) {
      ExperienceSetting.POST_SHADER -> (0..4).toList()
      ExperienceSetting.POST_MASK -> (-1..5).toList()
      else -> (0..8).toList()
    }
    ChoiceDialog(title = stringResource(PostShaders.title(field)),
      options = (if (gameKey != null) listOf<Int?>(null) else emptyList()).map {
        it to stringResource(R.string.extra_picture_use_global_value)
      } + values.map { it as Int? to valueLabel(field, it) },
      current = if (gameKey != null && field !in own) null else options[field],
      onPick = { update(mapOf(field to it)); choosing = null }, onDismiss = { choosing = null })
  }
}
