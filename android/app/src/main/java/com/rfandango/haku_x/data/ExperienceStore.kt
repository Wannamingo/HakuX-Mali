package com.rfandango.haku_x.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

/** UI writes atomically; the game reads afresh on resume. No per-frame disk access. */
class ExperienceStore(context: Context) {
  private val file = AtomicFile(File(context.filesDir, "experience-settings.json"))
  private fun read(): JSONObject = runCatching {
    file.openRead().bufferedReader().use { JSONObject(it.readText()) }
  }.getOrElse { JSONObject() }

  private fun section(json: JSONObject, game: String?) =
    if (game == null) json.optJSONObject("global") else json.optJSONObject("games")?.optJSONObject(game)

  fun own(game: String?): Map<ExperienceSetting, Int> {
    val data = section(read(), game) ?: return emptyMap()
    return ExperienceSetting.entries.mapNotNull { s ->
      if (data.has(s.key)) s to s.sanitize(data.optInt(s.key, s.default)) else null
    }.toMap()
  }

  fun effective(game: String? = null): ExperienceOptions {
    val data = read()
    val global = section(data, null)
    val local = game?.let { section(data, it) }
    return ExperienceOptions(ExperienceSetting.entries.associateWith { s ->
      s.sanitize(if (local?.has(s.key) == true) local.optInt(s.key, s.default)
        else global?.optInt(s.key, s.default) ?: s.default)
    })
  }

  fun set(setting: ExperienceSetting, value: Int?, game: String? = null) = setAll(mapOf(setting to value), game)

  /** A shader dialog commits once, after Apply; Cancel performs no file writes. */
  fun setAll(values: Map<ExperienceSetting, Int?>, game: String? = null) {
    if (values.isEmpty()) return
    update { json ->
      val parent = if (game == null) json else json.optJSONObject("games") ?: JSONObject().also { json.put("games", it) }
      val key = game ?: "global"
      val target = parent.optJSONObject(key) ?: JSONObject().also { parent.put(key, it) }
      for ((setting, value) in values) {
        if (value == null) target.remove(setting.key) else target.put(setting.key, setting.sanitize(value))
      }
    }
  }

  fun clear(game: String) = update { it.optJSONObject("games")?.remove(game); Unit }
  fun hotkeys(): Map<HotkeyAction, Int> {
    val data = read().optJSONObject("hotkeys") ?: return emptyMap()
    return HotkeyAction.entries.associateWith { data.optInt(it.name, 0).takeIf { key -> key in HOTKEY_KEYS } ?: 0 }
  }
  fun bind(action: HotkeyAction, code: Int) = update { json ->
    require(code == 0 || code in HOTKEY_KEYS)
    val data = json.optJSONObject("hotkeys") ?: JSONObject().also { json.put("hotkeys", it) }
    for (other in HotkeyAction.entries) if (other != action && code != 0 && data.optInt(other.name) == code) data.remove(other.name)
    data.put(action.name, code)
  }

  fun export(game: String?): String {
    val options = section(read(), game) ?: JSONObject()
    return JSONObject().put("format", "HakuX-Mali-settings").put("version", 1)
      .put("options", options).toString(2)
  }
  /** Imports only known, bounded settings. Files/URIs, drivers and saves are never imported. */
  fun importProfile(text: String, game: String?) {
    require(text.length <= 65536)
    val source = JSONObject(text)
    require(source.optString("format") == "HakuX-Mali-settings" && source.optInt("version") == 1)
    val options = source.getJSONObject("options")
    val known = ExperienceSetting.entries.mapNotNull { s ->
      if (options.has(s.key)) s to s.sanitize(options.getInt(s.key)) else null
    }
    update { json ->
      val parent = if (game == null) json else json.optJSONObject("games") ?: JSONObject().also { json.put("games", it) }
      val target = JSONObject(); known.forEach { (s,v) -> target.put(s.key, v) }
      parent.put(game ?: "global", target)
    }
  }

  private fun update(block: (JSONObject) -> Unit) {
    val data = read(); block(data)
    val output = file.startWrite()
    try { output.write(data.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
    catch (e: Exception) { file.failWrite(output); throw e }
  }
  companion object {
    // Start, shoulders, face buttons and stick clicks. Modifier itself is never assignable.
    val HOTKEY_KEYS = setOf(108, 102, 103, 96, 97, 99, 100, 106, 107)
  }
}
