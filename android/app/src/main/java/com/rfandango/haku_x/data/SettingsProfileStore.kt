package com.rfandango.haku_x.data

import android.content.Context
import org.json.JSONObject

/** Portable, bounded settings only: no document permissions, drivers, cheats or user data. */
class SettingsProfileStore(private val context: Context) {
  private val app = AppPrefs(context)
  private val extras = ExperienceStore(context)
  fun export(game: String?): String {
    val run = RunSettings(app, game?.let { GameOverrides(context, it) })
    val core = JSONObject()
    for (field in GameSetting.entries) core.put(field.key, when (field) {
      GameSetting.UNCAPPED -> run.uncapped.toString()
      GameSetting.FPS_LIMIT -> run.frameRateLimit.toString()
      GameSetting.RENDER_SCALE -> run.renderScale.toString()
      GameSetting.ASYNC_SHADERS -> run.asyncShaders.toString()
      GameSetting.PREBUILD_SHADERS -> run.prebuildShaders.toString()
      GameSetting.ASPECT -> run.aspectRatio
      GameSetting.FXAA -> run.fxaa.toString()
      GameSetting.UPSCALE -> run.upscaleFilter
      GameSetting.LANGUAGE -> run.language
      GameSetting.SCREEN_FORMAT -> run.screenFormat
      GameSetting.VIDEO_480P -> run.video480p.toString()
      GameSetting.VIDEO_720P -> run.video720p.toString()
      GameSetting.VIDEO_1080I -> run.video1080i.toString()
      GameSetting.PAL60 -> run.pal60.toString()
      GameSetting.AV_PACK -> run.avPack
      else -> run.picture(PictureControl.entries.first { it.setting == field }).toString()
    })
    val options = JSONObject()
    val effective = extras.effective(game)
    ExperienceSetting.entries.forEach { options.put(it.key, effective[it]) }
    return JSONObject().put("format", "HakuX-Mali-profile").put("version", 1)
      .put("core", core).put("options", options).toString(2)
  }

  fun importProfile(text: String, game: String?) {
    require(text.length <= 65536)
    val json = JSONObject(text)
    require(json.optString("format") == "HakuX-Mali-profile" && json.optInt("version") == 1)
    val core = json.getJSONObject("core")
    val values = GameSetting.entries.mapNotNull { field ->
      if (!core.has(field.key)) null else field to core.getString(field.key).also { require(valid(field, it)) }
    }
    val options = json.getJSONObject("options")
    for (field in ExperienceSetting.entries) if (options.has(field.key)) {
      require(options.getInt(field.key) in field.minimum..field.maximum)
    }
    // Everything is validated before changing either store.
    if (game != null) {
      val target = GameOverrides(context, game); target.clear()
      values.forEach { (field, value) -> target[field] = value }
    } else for ((field, value) in values) when (field) {
      GameSetting.UNCAPPED -> app.uncapped = value.toBoolean()
      GameSetting.FPS_LIMIT -> app.frameRateLimit = value.toInt()
      GameSetting.RENDER_SCALE -> app.renderScale = value.toInt()
      GameSetting.ASYNC_SHADERS -> app.asyncShaders = value.toBoolean()
      GameSetting.PREBUILD_SHADERS -> app.prebuildShaders = value.toBoolean()
      GameSetting.ASPECT -> app.aspectRatio = value
      GameSetting.FXAA -> app.fxaa = value.toBoolean()
      GameSetting.UPSCALE -> app.upscaleFilter = value
      GameSetting.LANGUAGE -> app.language = value
      GameSetting.SCREEN_FORMAT -> app.screenFormat = value
      GameSetting.VIDEO_480P -> app.video480p = value.toBoolean()
      GameSetting.VIDEO_720P -> app.video720p = value.toBoolean()
      GameSetting.VIDEO_1080I -> app.video1080i = value.toBoolean()
      GameSetting.PAL60 -> app.pal60 = value.toBoolean()
      GameSetting.AV_PACK -> app.avPack = value
      else -> app.setPicture(PictureControl.entries.first { it.setting == field }, value.toInt())
    }
    extras.importProfile(JSONObject().put("format", "HakuX-Mali-settings").put("version", 1)
      .put("options", options).toString(), game)
  }

  private fun valid(field: GameSetting, value: String): Boolean = when (field) {
    GameSetting.UNCAPPED, GameSetting.ASYNC_SHADERS, GameSetting.PREBUILD_SHADERS, GameSetting.FXAA,
    GameSetting.VIDEO_480P, GameSetting.VIDEO_720P, GameSetting.VIDEO_1080I, GameSetting.PAL60 -> value in listOf("true", "false")
    GameSetting.FPS_LIMIT -> value.toIntOrNull()?.let { it in 0..240 } == true
    GameSetting.RENDER_SCALE -> value.toIntOrNull()?.let { it in 1..4 } == true
    GameSetting.ASPECT -> value in listOf("auto", "4:3", "16:9", "stretch")
    GameSetting.UPSCALE -> value in listOf("bilinear", "sgsr")
    GameSetting.LANGUAGE -> value == AppPrefs.LANG_AUTO || value in AppPrefs.XBOX_LANGUAGES
    GameSetting.SCREEN_FORMAT -> value in listOf("normal", "letterbox", "widescreen")
    GameSetting.AV_PACK -> value in listOf("hdtv", "standard", "svideo")
    else -> PictureControl.entries.first { it.setting == field }.let { c -> value.toIntOrNull()?.let { it in c.minimum..c.maximum } == true }
  }
}
