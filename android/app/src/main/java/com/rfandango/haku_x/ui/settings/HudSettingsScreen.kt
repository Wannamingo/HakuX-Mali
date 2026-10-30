package com.rfandango.haku_x.ui.settings

import android.widget.Toast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.*
import com.rfandango.haku_x.ui.common.ChoiceDialog
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.Section
import com.rfandango.haku_x.ui.common.SubScreen
import com.rfandango.haku_x.ui.common.SwitchItem

@Composable
fun HudSettingsScreen(onBack: () -> Unit) {
  val context = LocalContext.current
  val store = remember { HudPreferences(context) }
  var options by remember { mutableStateOf(store.read(AppPrefs(context).showFps)) }
  var choosingPosition by remember { mutableStateOf(false) }
  fun update(value: HudOptions) {
    runCatching { store.write(value) }.onSuccess { options = value }.onFailure {
      Toast.makeText(context, R.string.extra_save_failed, Toast.LENGTH_LONG).show()
    }
  }
  SubScreen(stringResource(R.string.extra_hud), onBack) { padding ->
    Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Hint(stringResource(R.string.extra_hud_desc))
      SettingsGroup(true) {
        Section(stringResource(R.string.extra_hud_appearance))
        IntSettingSlider(stringResource(R.string.extra_hud_size), options.textSize, 8..18,
          label = { it.toString() }, onChange = { update(options.copy(textSize = it)) })
        ListItem(headlineContent = { Text(stringResource(R.string.extra_hud_position)) },
          supportingContent = { Text(stringResource(hudPositionLabel(options.position))) },
          modifier = Modifier.clickable { choosingPosition = true })
        IntSettingSlider(stringResource(R.string.extra_hud_background), options.background, 0..100,
          label = { context.getString(R.string.extra_percent, it) }, step = 5,
          onChange = { update(options.copy(background = it)) })
      }
      SettingsGroup(true) {
        Section(stringResource(R.string.extra_hud_temperatures))
        SwitchItem(stringResource(R.string.extra_hud_cpu), null, options.cpu) { update(options.copy(cpu = it)) }
        SwitchItem(stringResource(R.string.extra_hud_gpu), null, options.gpu) { update(options.copy(gpu = it)) }
        SwitchItem(stringResource(R.string.extra_hud_battery), null, options.battery) { update(options.copy(battery = it)) }
      }
    }
  }
  if (choosingPosition) ChoiceDialog(
    title = stringResource(R.string.extra_hud_position),
    options = HudPosition.entries.map { it to stringResource(hudPositionLabel(it)) }, current = options.position,
    onPick = { update(options.copy(position = it)); choosingPosition = false }, onDismiss = { choosingPosition = false },
  )
}

private fun hudPositionLabel(position: HudPosition) = when (position) {
  HudPosition.TOP_LEFT -> R.string.extra_hud_top_left
  HudPosition.TOP_RIGHT -> R.string.extra_hud_top_right
  HudPosition.BOTTOM_LEFT -> R.string.extra_hud_bottom_left
  HudPosition.BOTTOM_RIGHT -> R.string.extra_hud_bottom_right
}
