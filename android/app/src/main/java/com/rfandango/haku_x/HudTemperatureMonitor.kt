package com.rfandango.haku_x

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** No render/UI-thread I/O; stopped with the activity, sampled once every two seconds. */
class HudTemperatureMonitor(context: Context) : AutoCloseable {
  private val context = context.applicationContext
  private val worker = Executors.newSingleThreadScheduledExecutor { work ->
    Thread(work, "hakuX-temperature").apply { isDaemon = true }
  }
  private var polling: ScheduledFuture<*>? = null
  private data class Zone(val kind: TemperaturePolicy.Kind, val temperature: File)
  private var zones: List<Zone>? = null
  private var logged = false
  @Volatile var readings = TemperaturePolicy.Readings(null, null, null, null)
    private set
  val hudText get() = readings.hudText()

  @Synchronized fun start() {
    if (polling == null && !worker.isShutdown)
      polling = worker.scheduleWithFixedDelay({ sample() }, 0, 2, TimeUnit.SECONDS)
  }

  @Synchronized fun stop() {
    polling?.cancel(false)
    polling = null
  }

  @Synchronized override fun close() {
    stop()
    worker.shutdownNow()
  }

  private fun discover(): List<Zone> = runCatching {
    File("/sys/class/thermal").listFiles().orEmpty().mapNotNull { dir ->
      if (!dir.name.startsWith("thermal_zone")) return@mapNotNull null
      val kind = runCatching { TemperaturePolicy.kind(File(dir, "type").readText()) }.getOrNull()
      kind?.let { Zone(it, File(dir, "temp")) }
    }
  }.getOrDefault(emptyList())

  private fun sample() {
    // A failed/forbidden sysfs read never stops future battery samples.
    runCatching {
      val sensors = zones ?: discover().also { zones = it }
      val values = sensors.mapNotNull { zone ->
        runCatching { TemperaturePolicy.milliDegrees(zone.temperature.readText()) }.getOrNull()
          ?.let { TemperaturePolicy.Sample(zone.kind, it) }
      }
      val battery = runCatching {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
          ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
          ?.let(TemperaturePolicy::batteryTenths)
      }.getOrNull()
      readings = TemperaturePolicy.readings(values, battery)
      if (!logged) {
        Log.i("hakuX-temperature", "HUD sensors:$hudText (app access; 2 s interval)")
        logged = true
      }
    }
  }
}
