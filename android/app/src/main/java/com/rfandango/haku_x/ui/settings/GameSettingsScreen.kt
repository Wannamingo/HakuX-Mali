package com.rfandango.haku_x.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.AppPrefs
import com.rfandango.haku_x.data.GameOverrides
import com.rfandango.haku_x.data.GameSetting
import com.rfandango.haku_x.data.GraphicsOptions
import com.rfandango.haku_x.data.PictureControl
import com.rfandango.haku_x.ui.common.ChoiceDialog
import com.rfandango.haku_x.ui.common.ConfirmDialog
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.Section

/** A setting a game can override: its values (as stored) and how they read. */
private class Overridable(
  val setting: GameSetting,
  val title: Int,
  val values: List<String>,
  val global: (AppPrefs) -> String,
  val label: @Composable (String) -> String,
)

private val ON_OFF = listOf("true", "false")

@Composable
private fun onOff(value: String) =
  stringResource(if (value == "true") R.string.game_settings_on else R.string.game_settings_off)

private val SECTIONS: List<Pair<Int, List<Overridable>>> = listOf(
  R.string.settings_section_display to listOf(
    Overridable(GameSetting.FPS_LIMIT, R.string.settings_fps_limit, FRAME_LIMITS.map { it.toString() },
      { it.frameRateLimit.toString() }, { fpsLimitLabel(it.toInt()) }),
    Overridable(GameSetting.UNCAPPED, R.string.settings_uncap, ON_OFF, { it.uncapped.toString() }, { onOff(it) }),
    Overridable(GameSetting.ASPECT, R.string.settings_aspect, ASPECTS, { it.aspectRatio }, { stringResource(aspectLabel(it)) }),
  ),
  R.string.settings_section_graphics to listOf(
    Overridable(GameSetting.RENDER_SCALE, R.string.settings_scale, SCALES.map { "$it" }, { it.renderScale.toString() }, { scaleLabel(it.toInt()) }),
    Overridable(GameSetting.FXAA, R.string.settings_fxaa, ON_OFF, { it.fxaa.toString() }, { onOff(it) }),
    Overridable(GameSetting.UPSCALE, R.string.settings_upscale, UPSCALERS, { it.upscaleFilter }, { stringResource(upscaleLabel(it)) }),
    Overridable(GameSetting.ASYNC_SHADERS, R.string.settings_async_shaders, ON_OFF, { it.asyncShaders.toString() }, { onOff(it) }),
    Overridable(GameSetting.PREBUILD_SHADERS, R.string.settings_prebuild_shaders, ON_OFF, { it.prebuildShaders.toString() }, { onOff(it) }),
  ),
  R.string.settings_section_console to listOf(
    Overridable(GameSetting.LANGUAGE, R.string.settings_language, LANGUAGES, { it.language }, { languageLabel(it) }),
  ),
  R.string.settings_section_console_video to listOf(
    Overridable(GameSetting.SCREEN_FORMAT, R.string.settings_screen_format, FORMATS, { it.screenFormat }, { stringResource(formatLabel(it)) }),
    Overridable(GameSetting.VIDEO_480P, R.string.settings_480p, ON_OFF, { it.video480p.toString() }, { onOff(it) }),
    Overridable(GameSetting.VIDEO_720P, R.string.settings_720p, ON_OFF, { it.video720p.toString() }, { onOff(it) }),
    Overridable(GameSetting.VIDEO_1080I, R.string.settings_1080i, ON_OFF, { it.video1080i.toString() }, { onOff(it) }),
    Overridable(GameSetting.PAL60, R.string.settings_pal60, ON_OFF, { it.pal60.toString() }, { onOff(it) }),
    Overridable(GameSetting.AV_PACK, R.string.settings_av_cable, CABLES, { it.avPack }, { stringResource(cableLabel(it)) }),
  ),
)

/** One game's settings: each follows the app's until the game overrides it. */
@Composable
fun GameSettingsScreen(gameKey: String, title: String, onBack: () -> Unit, onOpenGlobal: (() -> Unit)? = null) {
  val context = LocalContext.current
  val prefs = remember { AppPrefs(context) }
  val overrides = remember(gameKey) { GameOverrides(context, gameKey) }
  val values = remember(gameKey) {
    mutableStateMapOf<GameSetting, String?>().apply { GameSetting.entries.forEach { put(it, overrides[it]) } }
  }
  var editing by remember { mutableStateOf<Overridable?>(null) }
  var resetting by remember { mutableStateOf(false) }
  var experienceRevision by remember { androidx.compose.runtime.mutableIntStateOf(0) }
  fun category(section: Int) = when (section) {
    R.string.settings_section_display -> SettingsCategory.DISPLAY
    R.string.settings_section_graphics -> SettingsCategory.GRAPHICS
    else -> SettingsCategory.SYSTEM
  }

  SettingsLayout(
    title = stringResource(R.string.game_settings_title),
    subtitle = title,
    onBack = onBack,
    entries = SECTIONS.flatMap { (section, items) -> items.map { item ->
      SettingsEntry(item.title, category(section), GlobalSettingsIndex.firstOrNull { it.title == item.title }?.aliases.orEmpty())
    } } + GlobalSettingsIndex.filter { it.title == R.string.extra_profile || it.title in PictureControl.entries.map { pictureLabel(it) } } +
      ExperienceIndex.filter { it.category != SettingsCategory.CONTROLS }.map {
        if (it.category == SettingsCategory.APP) it.copy(category = SettingsCategory.GRAPHICS) else it
      },
    categories = listOf(SettingsCategory.GRAPHICS, SettingsCategory.DISPLAY, SettingsCategory.AUDIO, SettingsCategory.SYSTEM, SettingsCategory.GAMES),
    scopeLabel = stringResource(R.string.ui_scope_game),
    otherScopeLabel = stringResource(R.string.ui_scope_global), onOtherScope = onOpenGlobal,
    actions = {
      TextButton(
        onClick = { resetting = true },
        enabled = true,
      ) { Text(stringResource(R.string.game_settings_reset)) }
    },
  ) { selected ->
    if (selected == SettingsCategory.GRAPHICS) androidx.compose.runtime.key(experienceRevision) {
      ExperienceSettingsSection(selected, gameKey, autoOnly = true)
    }
    Hint(stringResource(R.string.game_settings_desc))
    SettingsGroup(selected == SettingsCategory.GRAPHICS) {
      GraphicsProfileRow(GraphicsOptions(
        values[GameSetting.RENDER_SCALE]?.toIntOrNull() ?: prefs.renderScale,
        values[GameSetting.FXAA]?.toBooleanStrictOrNull() ?: prefs.fxaa,
        values[GameSetting.UPSCALE] ?: prefs.upscaleFilter,
        values[GameSetting.FPS_LIMIT]?.toIntOrNull() ?: prefs.frameRateLimit,
      )) { profile ->
        overrides.applyGraphicsProfile(profile)
        GameSetting.entries.forEach { values[it] = overrides[it] }
      }
    }
    for ((section, items) in SECTIONS) {
      SettingsGroup(selected == category(section)) {
        Section(stringResource(section))
        for (item in items) {
          val own = values[item.setting]
          SettingsTarget(item.title) {
            ListItem(
              headlineContent = { Text(stringResource(item.title)) },
              supportingContent = {
                if (own != null) {
                  Text(
                    stringResource(R.string.game_settings_this_game, item.label(own)),
                    color = MaterialTheme.colorScheme.primary,
                  )
                } else {
                  Text(stringResource(R.string.game_settings_global, item.label(item.global(prefs))))
                }
              },
              modifier = Modifier.clickable { editing = item },
            )
          }
        }
      }
    }
    SettingsGroup(selected == SettingsCategory.GRAPHICS) {
      PictureSettingsSection(prefs, game = true, values = values) { setting, value ->
        overrides[setting] = value; values[setting] = value
      }
    }
    androidx.compose.runtime.key(experienceRevision) { ExperienceSettingsSection(selected, gameKey) }
  }

  if (resetting) ConfirmDialog(
    title = stringResource(R.string.ui_reset_game_title), text = stringResource(R.string.ui_reset_game_desc),
    confirm = stringResource(R.string.game_settings_reset),
    onConfirm = { overrides.clear(); com.rfandango.haku_x.data.ExperienceStore(context).clear(gameKey)
      experienceRevision++; GameSetting.entries.forEach { values[it] = null }; resetting = false },
    onDismiss = { resetting = false },
  )

  editing?.let { item ->
    val useGlobal = stringResource(R.string.game_settings_use_global, item.label(item.global(prefs)))
    ChoiceDialog(
      title = stringResource(item.title),
      options = listOf<String?>(null).map { it to useGlobal } + item.values.map { it to item.label(it) },
      current = values[item.setting],
      onPick = { v ->
        overrides[item.setting] = v
        values[item.setting] = v
        editing = null
      },
      onDismiss = { editing = null },
    )
  }
}
