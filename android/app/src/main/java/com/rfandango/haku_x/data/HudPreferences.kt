package com.rfandango.haku_x.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

/** Read on resume, not per frame: SharedPreferences are cached separately by the native process. */
class HudPreferences(context: Context) {
  private val file = AtomicFile(File(context.filesDir, "hud-settings.json"))

  fun read(defaultVisible: Boolean = true): HudOptions = runCatching {
    val json = file.openRead().bufferedReader().use { JSONObject(it.readText()) }
    HudOptions(
      enabled = json.optBoolean("enabled", defaultVisible),
      textSize = json.optInt("size", 11),
      position = runCatching { HudPosition.valueOf(json.optString("position")) }.getOrDefault(HudPosition.TOP_LEFT),
      background = json.optInt("background", 40),
      cpu = json.optBoolean("cpu", true), gpu = json.optBoolean("gpu", true), battery = json.optBoolean("battery", true),
    ).sanitized()
  }.getOrDefault(HudOptions(enabled = defaultVisible))

  fun write(value: HudOptions) {
    val options = value.sanitized()
    val json = JSONObject().put("enabled", options.enabled).put("size", options.textSize)
      .put("position", options.position.name).put("background", options.background)
      .put("cpu", options.cpu).put("gpu", options.gpu).put("battery", options.battery)
    val output = file.startWrite()
    try { output.write(json.toString().toByteArray(Charsets.UTF_8)); file.finishWrite(output) }
    catch (e: Exception) { file.failWrite(output); throw e }
  }
}
