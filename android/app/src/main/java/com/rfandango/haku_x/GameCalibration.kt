package com.rfandango.haku_x

import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.rfandango.haku_x.data.*
import kotlin.math.max

/** One fresh native process per candidate. Only the explicit 30-second window
 * enters the metrics; checkpoint loading, shader prebuild and warmup do not. */
class GameCalibration(private val activity: NativeCoreActivity, private val restored: () -> Unit) {
  private val store = CalibrationStore(activity)
  private val session = activity.intent.getStringExtra(EXTRA_SESSION)?.let(store::read)
  private val restore = activity.intent.getBooleanExtra(EXTRA_RESTORE,false)
  private val handler = Handler(Looper.getMainLooper())
  private val temperatures = HudTemperatureMonitor(activity)
  private var started = SystemClock.uptimeMillis()
  private var warmAt = 0L
  private var measureAt = 0L
  private var measuring = false
  private var finished = false
  private var interrupted = false
  private var cpu: Double? = null
  private var gpu: Double? = null
  private var battery: Double? = null
  private val batteryLimit = ExperienceStore(activity).effective(session?.game)[ExperienceSetting.BATTERY_WARNING]
  val active get() = session != null && !finished
  val trial get() = active && !restore
  val view = LinearLayout(activity).apply {
    orientation=LinearLayout.VERTICAL; setPadding(16,8,16,8);setBackgroundColor(0xdd101b10.toInt())
  }
  private val line = TextView(activity).apply { setTextColor(-1);textSize=14f }
  init {
    view.addView(line)
    view.addView(Button(activity).apply {
      text=activity.getString(R.string.cal_cancel);isAllCaps=false
      setOnClickListener { cancel() }
    })
    view.visibility=if (trial) View.VISIBLE else View.GONE
  }
  fun resume() {
    if (!active) return
    if(restore) started=SystemClock.uptimeMillis()
    if (interrupted) { finish(null,activity.getString(R.string.cal_interrupted),cancelled=true);return }
    temperatures.start();handler.removeCallbacks(poll);handler.post(poll)
  }
  fun pause() {
    if (!active) return
    handler.removeCallbacks(poll);temperatures.stop()
    if (!restore) { interrupted=true; if(measuring) { NativeCoreActivity.nativeMeasureEnd();measuring=false } }
  }
  fun close() { handler.removeCallbacksAndMessages(null);temperatures.close() }
  fun cancel() { if (trial) finish(null,activity.getString(R.string.cal_cancelled),cancelled=true) }
  fun fatal(error:String):Boolean {
    if (!active) return false
    if (restore) {
      session!!.phase="restore_failed";session.error=error;store.write(session);leave()
    } else finish(null,error)
    return true
  }
  private val poll=object:Runnable {
    override fun run() {
      if (!active) return
      val s=session!!
      val now=SystemClock.uptimeMillis()
      val shown=NativeCoreActivity.nativeGetFrameShown()
      val shaders=NativeCoreActivity.nativeGetShaderBuild()
      if (restore) {
        if (shown) {
          s.phase="complete";store.write(s);store.cleanupCheckpoint(s.id)
          finished=true;view.visibility=View.GONE;temperatures.close();restored();return
        }
      } else {
        val t=temperatures.readings
        fun hottest(old:Double?,next:Float?)=next?.toDouble()?.let { max(old?:it,it) }?:old
        cpu=hottest(cpu,t.cpu?:t.soc);gpu=hottest(gpu,t.gpu);battery=hottest(battery,t.battery)
        val power=activity.getSystemService(PowerManager::class.java)
        val severe=Build.VERSION.SDK_INT>=29 && power.currentThermalStatus>=PowerManager.THERMAL_STATUS_SEVERE
        if(severe || (t.battery!=null && t.battery>=batteryLimit)) {
          finish(null,activity.getString(R.string.cal_hot),cancelled=true);return
        }
        if(warmAt==0L && shown && (shaders==null || shaders.size<2 || shaders[0]>=shaders[1])) warmAt=now
        if(warmAt!=0L && !measuring && now-warmAt>=CalibrationPolicy.WARMUP_MS) {
          NativeCoreActivity.nativeMeasureStart();measuring=true;measureAt=now
        }
        val elapsed=if(measuring) now-measureAt else if(warmAt!=0L) now-warmAt else 0
        val target=if(measuring) CalibrationPolicy.MEASURE_MS else CalibrationPolicy.WARMUP_MS
        val option=s.candidates[s.index]
        line.text=activity.getString(R.string.cal_progress,s.index+1,s.candidates.size,
          describe(option),activity.getString(if(measuring) R.string.cal_measuring else if(warmAt!=0L) R.string.cal_warming else R.string.cal_loading),
          (elapsed/1000).toInt(),(target/1000).toInt())
        if(measuring && elapsed>=CalibrationPolicy.MEASURE_MS) {
          val v=NativeCoreActivity.nativeMeasureEnd();measuring=false
          val metrics=if(v.size>=10) CalibrationMetrics(v[0],v[1].toLong(),v[2].toInt(),v[3],v[4],v[5],v[6],v[7],v[8],v[9].toLong(),cpu,gpu,battery) else null
          finish(metrics,if(metrics?.valid==true) null else activity.getString(R.string.cal_invalid));return
        }
      }
      if(warmAt==0L && now-started>CalibrationPolicy.BOOT_TIMEOUT_MS) {
        fatal(activity.getString(R.string.cal_timeout));return
      }
      handler.postDelayed(this,500)
    }
  }
  private fun finish(metrics:CalibrationMetrics?,error:String?,cancelled:Boolean=false) {
    if(!active)return
    if(measuring) { NativeCoreActivity.nativeMeasureEnd();measuring=false }
    val s=session!!
    s.runs=s.runs+CalibrationRun(s.candidates[s.index],metrics,error)
    s.index++
    if(cancelled || (s.index==1 && metrics?.valid!=true)) { s.error=error;s.phase="result" }
    else s.phase=if(s.index>=s.candidates.size) "result" else "ready"
    store.write(s);leave()
  }
  private fun leave() {
    finished=true;handler.removeCallbacksAndMessages(null);temperatures.close()
    activity.finish();android.os.Process.killProcess(android.os.Process.myPid())
  }
  companion object {
    const val EXTRA_SESSION="calibrationSession"
    const val EXTRA_RESTORE="calibrationRestore"
    const val EXTRA_REQUEST="calibrationRequest"
    fun describe(o:GraphicsOptions)="${o.scale}× · ${if(o.fxaa) "FXAA" else "FXAA OFF"} · ${if(o.upscale=="sgsr") "GSR" else "Bilinear"} · ${o.fpsLimit} FPS"
    fun options(activity:NativeCoreActivity):GraphicsOptions? {
      val s=activity.intent.getStringExtra(EXTRA_SESSION)?.let { CalibrationStore(activity).read(it) }?:return null
      return if(activity.intent.getBooleanExtra(EXTRA_RESTORE,false)) {
        if(s.phase=="applying") CalibrationPolicy.winner(s.runs)?.options?:s.original else s.original
      } else s.candidates.getOrNull(s.index)
    }
  }
}
