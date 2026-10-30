package com.rfandango.haku_x.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.GraphicsOptions
import com.rfandango.haku_x.data.GraphicsProfile
import com.rfandango.haku_x.ui.common.ChoiceDialog

@Composable
internal fun GraphicsProfileRow(options: GraphicsOptions, onApply: (GraphicsProfile) -> Unit) {
  var choosing by remember { mutableStateOf(false) }
  val current = GraphicsProfile.matching(options)
  SettingsTarget(R.string.extra_profile) {
    ListItem(headlineContent = { Text(stringResource(R.string.extra_profile)) },
      supportingContent = { Text(stringResource(profileLabel(current)) + ". " + stringResource(R.string.extra_profile_desc)) },
      modifier = Modifier.clickable { choosing = true })
  }
  if (choosing) ChoiceDialog<GraphicsProfile?>(
    title = stringResource(R.string.extra_profile),
    options = GraphicsProfile.entries.map { it to (stringResource(profileLabel(it)) + "\n" + stringResource(profileDescription(it))) },
    current = current,
    onPick = { profile -> if (profile != null) onApply(profile); choosing = false },
    onDismiss = { choosing = false },
  )
}

private fun profileLabel(profile: GraphicsProfile?) = when (profile) {
  GraphicsProfile.ECONOMY -> R.string.exp_economy
  GraphicsProfile.PERFORMANCE -> R.string.extra_profile_performance
  GraphicsProfile.BALANCED -> R.string.extra_profile_balanced
  GraphicsProfile.QUALITY -> R.string.extra_profile_quality
  null -> R.string.extra_profile_custom
}

private fun profileDescription(profile: GraphicsProfile) = when (profile) {
  GraphicsProfile.ECONOMY -> R.string.exp_economy_desc
  GraphicsProfile.PERFORMANCE -> R.string.extra_profile_performance_desc
  GraphicsProfile.BALANCED -> R.string.extra_profile_balanced_desc
  GraphicsProfile.QUALITY -> R.string.extra_profile_quality_desc
}
