package com.rfandango.haku_x.ui.settings

import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.*
import com.rfandango.haku_x.ui.common.ChoiceDialog
import com.rfandango.haku_x.ui.common.Hint
import com.rfandango.haku_x.ui.common.Section
import com.rfandango.haku_x.ui.common.SwitchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun experienceLabel(field: ExperienceSetting): Int = when (field) {
  ExperienceSetting.AUTO_PERFORMANCE -> R.string.exp_auto_performance
  ExperienceSetting.ANISOTROPY -> R.string.renderer_anisotropy
  ExperienceSetting.DYNAMIC -> R.string.renderer_dynamic
  ExperienceSetting.DYNAMIC_MIN -> R.string.renderer_dynamic_min
  ExperienceSetting.DYNAMIC_MAX -> R.string.renderer_dynamic_max
  ExperienceSetting.DYNAMIC_TARGET -> R.string.renderer_dynamic_target
  ExperienceSetting.POST_SHADER -> R.string.shader_title
  ExperienceSetting.POST_MASK, ExperienceSetting.POST_MASK_SIZE, ExperienceSetting.POST_MASK_STRENGTH,
  ExperienceSetting.POST_SCANLINES, ExperienceSetting.POST_GLOW, ExperienceSetting.POST_BLUR,
  ExperienceSetting.POST_CURVATURE -> PostShaders.title(field)
  ExperienceSetting.MOVIE_ASPECT -> R.string.renderer_movie_aspect
  ExperienceSetting.VOLUME -> R.string.exp_volume
  ExperienceSetting.MUTE -> R.string.exp_mute
  ExperienceSetting.AUDIO_BUFFER -> R.string.exp_audio_buffer
  ExperienceSetting.HRTF -> R.string.exp_hrtf
  ExperienceSetting.CROP_LEFT -> R.string.exp_crop_left
  ExperienceSetting.CROP_RIGHT -> R.string.exp_crop_right
  ExperienceSetting.CROP_TOP -> R.string.exp_crop_top
  ExperienceSetting.CROP_BOTTOM -> R.string.exp_crop_bottom
  ExperienceSetting.ZOOM -> R.string.exp_zoom
  ExperienceSetting.SHIFT_X -> R.string.exp_shift_x
  ExperienceSetting.SHIFT_Y -> R.string.exp_shift_y
  ExperienceSetting.REFRESH_RATE -> R.string.exp_refresh
  ExperienceSetting.AUTO_SAVE -> R.string.exp_autosave
  ExperienceSetting.OFFER_RESUME -> R.string.exp_resume
  ExperienceSetting.THERMAL_WARNING -> R.string.exp_thermal
  ExperienceSetting.SOC_WARNING -> R.string.exp_soc_limit
  ExperienceSetting.BATTERY_WARNING -> R.string.exp_battery_limit
}

internal fun experienceCategory(field: ExperienceSetting) = when (field) {
  ExperienceSetting.AUTO_PERFORMANCE,ExperienceSetting.ANISOTROPY,ExperienceSetting.DYNAMIC,ExperienceSetting.DYNAMIC_MIN,
  ExperienceSetting.DYNAMIC_MAX,ExperienceSetting.DYNAMIC_TARGET -> SettingsCategory.GRAPHICS
  ExperienceSetting.VOLUME, ExperienceSetting.MUTE, ExperienceSetting.AUDIO_BUFFER, ExperienceSetting.HRTF -> SettingsCategory.AUDIO
  ExperienceSetting.AUTO_SAVE, ExperienceSetting.OFFER_RESUME -> SettingsCategory.GAMES
  else -> SettingsCategory.DISPLAY
}

internal val ExperienceIndex = ExperienceSetting.entries.map {
  SettingsEntry(experienceLabel(it), experienceCategory(it), it.key.replace('_', ' ') +
    if (it in PostShaders.allFields) " shader mask crt lcd oled pixels scanline glow blur curvature шейдер маска пиксели строки свечение размытие выпуклость" else "")
} + listOf(SettingsEntry(R.string.exp_hotkeys, SettingsCategory.CONTROLS, "hotkey shortcut screenshot quick save сочетания снимок быстрое сохранение"),
  SettingsEntry(R.string.exp_profiles, SettingsCategory.APP, "profile export import copy профиль экспорт импорт копировать"))

@Composable
internal fun ColumnScope.ExperienceSettingsSection(category: SettingsCategory, gameKey: String? = null, autoOnly: Boolean = false) {
  val context = LocalContext.current
  val store = remember { ExperienceStore(context) }
  var revision by remember { mutableIntStateOf(0) }
  val effective = remember(revision) { store.effective(gameKey) }
  val own = remember(revision) { store.own(gameKey) }
  var capabilities by remember { mutableStateOf<RendererCapabilities?>(null) }
  LaunchedEffect(Unit) { capabilities=withContext(Dispatchers.IO) { RendererCapabilitiesStore(context).read() } }
  var choosing by remember { mutableStateOf<ExperienceSetting?>(null) }
  fun update(field: ExperienceSetting, value: Int?) {
    runCatching { store.set(field, value, gameKey) }.onSuccess { revision++ }
      .onFailure { Toast.makeText(context, R.string.extra_save_failed, Toast.LENGTH_LONG).show() }
  }
  if (!autoOnly && category == SettingsCategory.DISPLAY) PostShaderSettingsSection(gameKey)
  val fields = ExperienceSetting.entries.filter { it !in PostShaders.allFields && experienceCategory(it) == category &&
    (if (autoOnly) it == ExperienceSetting.AUTO_PERFORMANCE else it != ExperienceSetting.AUTO_PERFORMANCE) }
  if (fields.isNotEmpty()) SettingsGroup(true) {
    Section(stringResource(when (category) {
      SettingsCategory.AUDIO -> R.string.exp_audio
      SettingsCategory.GRAPHICS -> if(autoOnly) R.string.exp_auto_heading else R.string.renderer_heading
      SettingsCategory.GAMES -> R.string.exp_states
      else -> R.string.exp_framing
    }))
    Hint(stringResource(when (category) {
      SettingsCategory.AUDIO -> R.string.exp_audio_desc
      SettingsCategory.GRAPHICS -> if(autoOnly) R.string.exp_auto_performance_desc else R.string.settings_applies_next_start
      else -> R.string.exp_runtime_desc
    }))
    if (category == SettingsCategory.DISPLAY) TextButton(onClick = {
      for (field in listOf(ExperienceSetting.CROP_LEFT,ExperienceSetting.CROP_RIGHT,ExperienceSetting.CROP_TOP,
        ExperienceSetting.CROP_BOTTOM,ExperienceSetting.ZOOM,ExperienceSetting.SHIFT_X,ExperienceSetting.SHIFT_Y))
        update(field, if (gameKey != null) null else field.default)
    }) { Text(stringResource(R.string.exp_reset_framing)) }
    for (field in fields) {
      val value = effective[field]
      val title = stringResource(experienceLabel(field))
      val inherited = gameKey != null && field !in own
      SettingsTarget(experienceLabel(field)) {
        when (field) {
          ExperienceSetting.DYNAMIC_MIN,ExperienceSetting.DYNAMIC_MAX,ExperienceSetting.DYNAMIC_TARGET,
          ExperienceSetting.POST_SHADER,ExperienceSetting.MOVIE_ASPECT,ExperienceSetting.ANISOTROPY,ExperienceSetting.AUDIO_BUFFER, ExperienceSetting.REFRESH_RATE -> ListItem(
            headlineContent = { Text(title) }, supportingContent = { Text(
              (if (inherited) stringResource(R.string.exp_inherited) + " " else "") +
                if(field==ExperienceSetting.DYNAMIC_TARGET) "$value FPS"
                else if(field==ExperienceSetting.DYNAMIC_MIN||field==ExperienceSetting.DYNAMIC_MAX) "${value}×"
                else if(field==ExperienceSetting.POST_SHADER) stringResource(PostShaders.label(value))+" · "+stringResource(R.string.shader_desc)
                else if(field==ExperienceSetting.MOVIE_ASPECT) stringResource(movieLabel(value))+" · "+stringResource(R.string.renderer_movie_desc)
                else if(field==ExperienceSetting.ANISOTROPY) (if(value==0) stringResource(R.string.renderer_original) else "${value}×") +
                  if(capabilities==null) " · "+stringResource(R.string.renderer_caps_unknown) else ""
                else if (field == ExperienceSetting.AUDIO_BUFFER) stringResource(bufferLabel(value))
                else if (value == 0) stringResource(R.string.exp_auto) else "$value Hz"
            ) }, modifier = Modifier.clickable { choosing = field })
          ExperienceSetting.DYNAMIC,ExperienceSetting.AUTO_PERFORMANCE, ExperienceSetting.MUTE, ExperienceSetting.HRTF, ExperienceSetting.AUTO_SAVE,
          ExperienceSetting.OFFER_RESUME, ExperienceSetting.THERMAL_WARNING -> SwitchItem(title,
            when (field) {
              ExperienceSetting.AUTO_PERFORMANCE -> stringResource(R.string.settings_applies_next_start)
              ExperienceSetting.DYNAMIC -> stringResource(R.string.renderer_dynamic_desc)
              ExperienceSetting.AUTO_SAVE -> stringResource(R.string.exp_autosave_desc)
              ExperienceSetting.OFFER_RESUME -> stringResource(R.string.exp_resume_desc)
              ExperienceSetting.THERMAL_WARNING -> stringResource(R.string.exp_thermal_desc)
              ExperienceSetting.HRTF -> stringResource(R.string.settings_applies_next_start)
              else -> if (inherited) stringResource(R.string.exp_inherited) else null
            }, value != 0) { update(field, if (it) 1 else 0) }
          else -> IntSettingSlider(title, value, field.minimum..field.maximum,
            step = if (field == ExperienceSetting.VOLUME || field == ExperienceSetting.ZOOM) 5 else 1,
            label = { if (field in listOf(ExperienceSetting.SOC_WARNING, ExperienceSetting.BATTERY_WARNING)) "$it°C" else "$it%" },
            note = if (inherited) stringResource(R.string.exp_inherited) else null,
            onChange = { update(field, it) })
        }
      }
      if (gameKey != null && field in own) TextButton(onClick = { update(field, null) }) {
        Text(stringResource(R.string.extra_picture_use_global_value))
      }
    }
    if(autoOnly) CalibrationSection(gameKey)
  }
  if (!autoOnly && category == SettingsCategory.CONTROLS && gameKey == null) HotkeysSection(store)
  if (!autoOnly && (category == SettingsCategory.APP || (gameKey != null && category == SettingsCategory.GRAPHICS))) {
    SettingsGroup(true) { SettingsProfilesSection(gameKey) { revision++ } }
  }
  choosing?.let { field ->
    val values = if(field==ExperienceSetting.DYNAMIC_TARGET) listOf(30,60,90,120)
    else if(field==ExperienceSetting.DYNAMIC_MIN) listOf(1,2)
    else if(field==ExperienceSetting.DYNAMIC_MAX) (effective[ExperienceSetting.DYNAMIC_MIN]..2).toList()
    else if(field==ExperienceSetting.POST_SHADER) (0..4).toList()
    else if(field==ExperienceSetting.MOVIE_ASPECT) listOf(0,1,2,3)
    else if(field==ExperienceSetting.ANISOTROPY) capabilities?.anisotropyChoices?:listOf(0)
    else if (field == ExperienceSetting.AUDIO_BUFFER) listOf(0, 1, 2) else {
      @Suppress("DEPRECATION") val display = context.getSystemService(android.view.WindowManager::class.java).defaultDisplay
      listOf(0) + display.supportedModes.filter {
        it.physicalWidth == display.mode.physicalWidth && it.physicalHeight == display.mode.physicalHeight
      }.map { it.refreshRate.toInt() }.distinct().sorted()
    }
    ChoiceDialog(title = stringResource(experienceLabel(field)), options =
      (if (gameKey != null) listOf<Int?>(null) else emptyList()) .map { it to stringResource(R.string.extra_picture_use_global_value) } +
        values.map { it as Int? to if(field==ExperienceSetting.DYNAMIC_TARGET) "$it FPS"
          else if(field==ExperienceSetting.DYNAMIC_MIN||field==ExperienceSetting.DYNAMIC_MAX) "${it}×"
          else if(field==ExperienceSetting.POST_SHADER) stringResource(PostShaders.label(it))
          else if(field==ExperienceSetting.MOVIE_ASPECT) stringResource(movieLabel(it))
          else if(field==ExperienceSetting.ANISOTROPY) (if(it==0) stringResource(R.string.renderer_original) else "${it}×")
          else if (field == ExperienceSetting.AUDIO_BUFFER) stringResource(bufferLabel(it))
          else if (it == 0) stringResource(R.string.exp_auto) else "$it Hz" },
      current = if (gameKey != null && field !in own) null else effective[field],
      onPick = { update(field, it); choosing = null }, onDismiss = { choosing = null })
  }
}

@Composable private fun CalibrationSection(gameKey:String?) {
  val context=LocalContext.current
  var measured by remember(gameKey) { mutableStateOf<CalibrationRun?>(null) }
  var pending by remember { mutableStateOf<CalibrationSession?>(null) }
  LaunchedEffect(gameKey) {
    withContext(Dispatchers.IO) {
      val store=CalibrationStore(context)
      measured=gameKey?.let(store::measured);pending=store.pending()?.takeIf { it.phase !in listOf("complete","discarded") }
    }
  }
  val result=measured
  Hint(if(result==null) stringResource(R.string.cal_not_measured)
    else stringResource(R.string.cal_saved,com.rfandango.haku_x.GameCalibration.describe(result.options),result.metrics!!.fps,result.metrics.p95Ms))
  TextButton(onClick={
    val active=CalibrationStore.activeGame(context)
    if(gameKey!=null && active==gameKey) context.startActivity(android.content.Intent(context,com.rfandango.haku_x.NativeCoreActivity::class.java)
      .putExtra(com.rfandango.haku_x.GameCalibration.EXTRA_REQUEST,gameKey)
      .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
    else Toast.makeText(context,R.string.cal_open_game,Toast.LENGTH_LONG).show()
  }) { Text(stringResource(if(result==null) R.string.cal_title else R.string.cal_repeat)) }
  if(pending!=null) TextButton(onClick={
    val active=CalibrationStore.activeGame(context)
    if(active!=null) context.startActivity(android.content.Intent(context,com.rfandango.haku_x.NativeCoreActivity::class.java)
      .putExtra(com.rfandango.haku_x.GameCalibration.EXTRA_REQUEST,active)
      .addFlags(android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
    else context.startActivity(android.content.Intent(context,com.rfandango.haku_x.CalibrationActivity::class.java)
      .putExtra(com.rfandango.haku_x.GameCalibration.EXTRA_SESSION,pending!!.id))
  }) { Text(stringResource(R.string.cal_recover)) }
}

private fun bufferLabel(value: Int) = when (value) {
  0 -> R.string.exp_audio_low; 2 -> R.string.exp_audio_stable; else -> R.string.exp_audio_balanced
}
private fun movieLabel(value:Int)=when(value) {
  1->R.string.renderer_movie_43;2->R.string.renderer_movie_169;3->R.string.renderer_movie_auto;else->R.string.renderer_movie_original
}
internal fun hotkeyLabel(action: HotkeyAction) = when (action) {
  HotkeyAction.MENU -> R.string.exp_key_menu
  HotkeyAction.SCREENSHOT -> R.string.exp_key_screenshot
  HotkeyAction.QUICK_SAVE -> R.string.exp_key_save
  HotkeyAction.QUICK_LOAD -> R.string.exp_key_load
  HotkeyAction.SAVE_MENU -> R.string.overlay_menu_save_state
  HotkeyAction.LOAD_MENU -> R.string.overlay_menu_load_state
  HotkeyAction.MOVIE_ASPECT -> R.string.renderer_movie_toggle
}

@Composable private fun HotkeysSection(store: ExperienceStore) {
  val context = LocalContext.current
  var bindings by remember { mutableStateOf(store.hotkeys()) }
  var editing by remember { mutableStateOf<HotkeyAction?>(null) }
  SettingsGroup(true) {
    SettingsTarget(R.string.exp_hotkeys) {
      Column { Section(stringResource(R.string.exp_hotkeys)); Hint(stringResource(R.string.exp_hotkeys_desc)) }
    }
    for (action in HotkeyAction.entries) ListItem(headlineContent = { Text(stringResource(hotkeyLabel(action))) },
      supportingContent = { Text(if (bindings[action] == null || bindings[action] == 0) stringResource(R.string.exp_unassigned)
        else "Select + " + KeyEvent.keyCodeToString(bindings.getValue(action)).removePrefix("KEYCODE_BUTTON_")) },
      modifier = Modifier.clickable { editing = action })
  }
  editing?.let { action -> ChoiceDialog(title = stringResource(hotkeyLabel(action)),
    options = listOf(0 to stringResource(R.string.exp_unassigned)) + ExperienceStore.HOTKEY_KEYS.map {
      it to "Select + " + KeyEvent.keyCodeToString(it).removePrefix("KEYCODE_BUTTON_") }, current = bindings[action] ?: 0,
    onPick = { code -> runCatching { store.bind(action, code) }.onSuccess { bindings = store.hotkeys(); editing = null }
      .onFailure { Toast.makeText(context, R.string.extra_save_failed, Toast.LENGTH_LONG).show() } },
    onDismiss = { editing = null }) }
}

@Composable private fun SettingsProfilesSection(gameKey: String?, changed: () -> Unit) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  fun report(success: Boolean) = Toast.makeText(context, if (success) R.string.exp_profile_done else R.string.exp_profile_failed, Toast.LENGTH_LONG).show()
  val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
    if (uri != null) scope.launch { report(withContext(Dispatchers.IO) { runCatching {
      context.contentResolver.openOutputStream(uri, "wt")!!.bufferedWriter().use { it.write(SettingsProfileStore(context).export(gameKey)) }
    }.isSuccess }) }
  }
  val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
    if (uri != null) scope.launch {
      val success = withContext(Dispatchers.IO) { runCatching {
        val text = context.contentResolver.openInputStream(uri)!!.use { stream ->
          val bytes = ByteArray(65537)
          var size = 0
          while (size < bytes.size) { val count = stream.read(bytes, size, bytes.size - size); if (count < 0) break; size += count }
          require(size <= 65536); bytes.copyOf(size).toString(Charsets.UTF_8)
        }
        SettingsProfileStore(context).importProfile(text, gameKey)
      }.isSuccess }
      report(success)
      if (success) {
        changed()
        (context as? android.app.Activity)?.recreate()
      }
    }
  }
  SettingsTarget(R.string.exp_profiles) {
    ListItem(headlineContent = { Text(stringResource(R.string.exp_profiles)) }, supportingContent = { Text(stringResource(R.string.exp_profiles_desc)) })
  }
  TextButton(onClick = { export.launch("HakuX-Mali-${gameKey ?: "global"}.json") }) { Text(stringResource(R.string.exp_export)) }
  TextButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text(stringResource(R.string.exp_import)) }
}
