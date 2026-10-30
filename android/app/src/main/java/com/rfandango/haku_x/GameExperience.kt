package com.rfandango.haku_x

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import android.view.KeyEvent
import android.widget.Toast
import com.rfandango.haku_x.data.*
import java.io.File
import java.util.Locale

/** Running-game conveniences, kept separate from emulation/pause ownership. */
class GameExperience(private val activity: NativeCoreActivity, private val capture: ((Bitmap?) -> Unit) -> Unit,
  private val action: (HotkeyAction) -> Unit) {
  var options = ExperienceOptions(); private set
  private var bindings = emptyMap<HotkeyAction, Int>()
  private class Keys { val gate = HotkeyGate(); var modifier: KeyEvent? = null; var forwarded = false }
  private val keys = mutableMapOf<Int, Keys>()
  private var lastWarning = 0L
  private var wasHot = false

  fun reload(game: String) {
    val store = ExperienceStore(activity)
    options = store.effective(game); bindings = store.hotkeys()
    NativeCoreActivity.nativeSetOutputVolume(options[ExperienceSetting.VOLUME], options.enabled(ExperienceSetting.MUTE))
    NativeCoreActivity.nativeSetFraming(listOf(ExperienceSetting.CROP_LEFT, ExperienceSetting.CROP_RIGHT,
      ExperienceSetting.CROP_TOP, ExperienceSetting.CROP_BOTTOM, ExperienceSetting.ZOOM,
      ExperienceSetting.SHIFT_X, ExperienceSetting.SHIFT_Y).map { options[it] }.toIntArray())
    @Suppress("DEPRECATION") val display = activity.windowManager.defaultDisplay
    val current = display.mode
    val hz = options[ExperienceSetting.REFRESH_RATE]
    val selected = display.supportedModes.filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
      .firstOrNull { kotlin.math.abs(it.refreshRate - hz) < 1f }
    activity.window.attributes = activity.window.attributes.apply {
      preferredDisplayModeId = selected?.modeId ?: 0
      preferredRefreshRate = selected?.refreshRate ?: 0f
    }
  }

  /** Only real gamepads reach this; every prefix is delayed separately per device. */
  fun onKey(event: KeyEvent, forward: (KeyEvent) -> Unit): Boolean {
    if (bindings.values.none { it != 0 }) return false
    if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false
    val session = keys.getOrPut(event.deviceId) { Keys() }
    if (event.keyCode == HotkeyGate.MODIFIER && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
      session.modifier = KeyEvent(event); session.forwarded = false
    }
    val result = session.gate.event(event.keyCode, event.action == KeyEvent.ACTION_DOWN, event.repeatCount != 0, bindings)
    if (result.flushModifier) { session.modifier?.let(forward); session.forwarded = true }
    if (event.keyCode == HotkeyGate.MODIFIER && event.action == KeyEvent.ACTION_UP) {
      session.modifier = null; session.forwarded = false
    }
    if (!event.isCanceled) result.action?.let(action)
    return result.consume
  }

  fun clearKeys(forward: (KeyEvent) -> Unit) {
    for (session in keys.values) {
      if (session.forwarded) session.modifier?.let { down -> forward(KeyEvent.changeAction(down, KeyEvent.ACTION_UP)) }
      session.gate.clear()
    }
    keys.clear()
  }

  fun checkTemperature(readings: TemperaturePolicy.Readings) {
    if (!options.enabled(ExperienceSetting.THERMAL_WARNING)) return
    val cpu = listOfNotNull(readings.cpu, readings.soc).maxOrNull()
    val battery = readings.battery
    val socLimit = options[ExperienceSetting.SOC_WARNING]
    val batteryLimit = options[ExperienceSetting.BATTERY_WARNING]
    val hot = (cpu != null && cpu >= socLimit) || (battery != null && battery >= batteryLimit)
    val now = SystemClock.elapsedRealtime()
    if (hot && (!wasHot || now - lastWarning > 300_000L)) {
      val values = listOfNotNull(if (cpu != null && cpu >= socLimit) "CPU/SoC %.0f°C".format(Locale.ROOT, cpu) else null,
        if (battery != null && battery >= batteryLimit) "BAT %.0f°C".format(Locale.ROOT, battery) else null).joinToString(", ")
      Toast.makeText(activity, activity.getString(R.string.exp_thermal_toast, values), Toast.LENGTH_LONG).show()
      wasHot = true; lastWarning = now
    } else if ((cpu == null || cpu < socLimit - 3) && (battery == null || battery < batteryLimit - 3)) wasHot = false
  }

  fun screenshot() {
    capture { bitmap ->
      if (bitmap == null) { Toast.makeText(activity, R.string.exp_screenshot_failed, Toast.LENGTH_LONG).show(); return@capture }
      Thread {
        val result = runCatching {
          val name = "HakuX-Mali-${System.currentTimeMillis()}.png"
          if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
              put(MediaStore.Images.Media.DISPLAY_NAME, name); put(MediaStore.Images.Media.MIME_TYPE, "image/png")
              put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/HakuX-Mali"); put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = activity.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
            try {
              resolver.openOutputStream(uri)!!.use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
              resolver.update(uri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
          } else {
            val folder = File(activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "HakuX-Mali").apply { mkdirs() }
            File(folder, name).outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
          }
        }
        bitmap.recycle()
        activity.runOnUiThread { Toast.makeText(activity, if (result.isSuccess) R.string.exp_screenshot_saved else R.string.exp_screenshot_failed, Toast.LENGTH_LONG).show() }
      }.start()
    }
  }
}
