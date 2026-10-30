package com.rfandango.haku_x.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** Persist on release, including keyboard/controller changes, rather than on every drag sample. */
@Composable
internal fun IntSettingSlider(title: String, value: Int, range: IntRange, label: (Int) -> String,
  note: String? = null, step: Int = 1, onChange: (Int) -> Unit) {
  var draft by remember(value) { mutableFloatStateOf(value.toFloat()) }
  Column {
    ListItem(headlineContent = { Text(title) }, supportingContent = note?.let { { Text(it) } },
      trailingContent = { Text(label(draft.roundToInt())) })
    Slider(value = draft, onValueChange = { draft = it },
      onValueChangeFinished = { onChange(draft.roundToInt().coerceIn(range)) },
      valueRange = range.first.toFloat()..range.last.toFloat(), steps = ((range.last - range.first) / step - 1).coerceAtLeast(0),
      modifier = Modifier.padding(horizontal = 24.dp).semantics { contentDescription = title })
  }
}
