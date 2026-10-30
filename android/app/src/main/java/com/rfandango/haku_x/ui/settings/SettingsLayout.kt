package com.rfandango.haku_x.ui.settings

import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.rfandango.haku_x.R
import com.rfandango.haku_x.ui.common.SubScreen

enum class SettingsCategory(val label: Int) {
  GRAPHICS(R.string.ui_category_graphics), DISPLAY(R.string.ui_category_display),
  CONTROLS(R.string.ui_category_controls), GAMES(R.string.ui_category_games),
  SYSTEM(R.string.ui_category_system), APP(R.string.ui_category_app),
}

data class SettingsEntry(val title: Int, val category: SettingsCategory, val aliases: String = "")
private data class SettingsJump(val title: Int, val generation: Int)
private val LocalSettingsJump = staticCompositionLocalOf<SettingsJump?> { null }

/** Fixed categories/search, with independent retained scroll positions for each category. */
@Composable
fun SettingsLayout(
  title: String,
  onBack: () -> Unit,
  entries: List<SettingsEntry>,
  categories: List<SettingsCategory> = SettingsCategory.entries,
  subtitle: String? = null,
  snackbar: SnackbarHostState? = null,
  scopeLabel: String = stringResource(R.string.ui_scope_global),
  otherScopeLabel: String? = null,
  onOtherScope: (() -> Unit)? = null,
  actions: @Composable RowScope.() -> Unit = {},
  content: @Composable ColumnScope.(SettingsCategory) -> Unit,
) {
  var selected by rememberSaveable { mutableStateOf(categories.first().name) }
  val category = categories.firstOrNull { it.name == selected } ?: categories.first()
  var searching by rememberSaveable { mutableStateOf(false) }
  var query by rememberSaveable { mutableStateOf("") }
  var generation by remember { mutableIntStateOf(0) }
  var jump by remember { mutableStateOf<SettingsJump?>(null) }
  val searchFocus = remember { FocusRequester() }
  LaunchedEffect(searching) { if (searching) searchFocus.requestFocus() }
  // Keep scroll positions without reusing the categories' layout/render nodes.
  // A reused scrolling subtree can retain the previous category's drawing even
  // though its semantics and click targets already belong to the new category.
  val scrollStates = categories.map { tab ->
    key(tab.name) { tab to rememberScrollState() }
  }.toMap()
  val keyboard = LocalSoftwareKeyboardController.current
  val focus = LocalFocusManager.current
  fun closeSearch() { searching = false; query = ""; keyboard?.hide(); focus.clearFocus() }
  fun toggleSearch() { if (searching) closeSearch() else { searching = true; query = "" } }
  fun select(value: SettingsCategory) { selected = value.name; if (searching) closeSearch(); jump = null }
  val translated = entries.map { it to stringResource(it.title) }
  val matches = translated.filter { (entry, label) -> query.isBlank() || SettingsSearch.matches(query, label, entry.aliases) }
  val back = { if (searching) closeSearch() else onBack() }
  BackHandler(enabled = searching) { back() }

  SubScreen(title, back, subtitle, snackbar, actions = {
    actions()
    IconButton(onClick = ::toggleSearch) {
      Icon(if (searching) Icons.Default.Close else Icons.Default.Search,
        stringResource(if (searching) R.string.ui_search_close else R.string.ui_search_settings))
    }
  }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).imePadding().onPreviewKeyEvent { event ->
      if (event.type != KeyEventType.KeyDown) false else when (event.nativeKeyEvent.keyCode) {
        KeyEvent.KEYCODE_BUTTON_L1, KeyEvent.KEYCODE_BUTTON_R1 -> {
          val direction = if (event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BUTTON_L1) -1 else 1
          select(categories[(categories.indexOf(category) + direction + categories.size) % categories.size]); true
        }
        KeyEvent.KEYCODE_BUTTON_Y -> { toggleSearch(); true }
        KeyEvent.KEYCODE_BUTTON_B -> { back(); true }
        else -> false
      }
    }) {
      if (!searching) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(scopeLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(vertical = 12.dp))
        if (onOtherScope != null && otherScopeLabel != null) TextButton(onClick = onOtherScope) { Text(otherScopeLabel) }
      }
      if (searching) OutlinedTextField(query, { query = it }, singleLine = true,
        label = { Text(stringResource(R.string.ui_search_settings)) },
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide(); focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(searchFocus))
      if (!searching) Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (tab in categories) FilterChip(category == tab, onClick = { select(tab) }, label = { Text(stringResource(tab.label)) })
      }
      if (searching) {
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          if (matches.isEmpty()) item { Text(stringResource(R.string.ui_search_empty), Modifier.padding(16.dp)) }
          items(matches, key = { it.first.title }) { (entry, label) ->
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
              ListItem(headlineContent = { Text(label) }, supportingContent = { Text(stringResource(entry.category.label)) },
                modifier = Modifier.clickable {
                  selected = entry.category.name; closeSearch()
                  generation++; jump = SettingsJump(entry.title, generation)
                })
            }
          }
        }
      } else key(category.name) {
        val scroll = scrollStates.getValue(category)
        CompositionLocalProvider(LocalSettingsJump provides jump) {
          Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content(category)
            Spacer(Modifier.height(12.dp))
          }
        }
      }
    }
  }
}

@Composable
fun SettingsGroup(shown: Boolean, content: @Composable ColumnScope.() -> Unit) {
  if (!shown) return
  val colors = MaterialTheme.colorScheme
  Surface(shape = RoundedCornerShape(22.dp), color = colors.surfaceContainerLow,
    border = BorderStroke(1.dp, colors.outlineVariant), modifier = Modifier.fillMaxWidth()) {
    MaterialTheme(colorScheme = colors.copy(surface = colors.surfaceContainerLow)) {
      Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), content = content)
    }
  }
}

@Composable
fun SettingsTarget(title: Int, content: @Composable () -> Unit) {
  val jump = LocalSettingsJump.current
  val selected = jump?.title == title
  val requester = remember { BringIntoViewRequester() }
  val keyboardOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
  LaunchedEffect(selected, jump?.generation, keyboardOpen) {
    if (selected && !keyboardOpen) { withFrameNanos { }; requester.bringIntoView() }
  }
  val shape = RoundedCornerShape(16.dp)
  Box(Modifier.fillMaxWidth().bringIntoViewRequester(requester).clip(shape)
    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
      .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = .3f)) else Modifier)) { content() }
}
