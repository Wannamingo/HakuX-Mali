package com.rfandango.haku_x

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.rfandango.haku_x.data.*
import java.util.Locale

/** Main-process coordinator stays below the trial. Returning without a written
 * result means a crashed/interrupted candidate, never a successful measurement. */
class CalibrationActivity:Activity() {
  override fun attachBaseContext(base:Context)=super.attachBaseContext(UiLanguage.wrap(base))
  private val handler=Handler(Looper.getMainLooper())
  private val store by lazy { CalibrationStore(this) }
  private var foreground=false
  private var launched=false
  private var waitAt=0L
  private var session:CalibrationSession?=null
  private lateinit var column:LinearLayout
  override fun onCreate(state:Bundle?) {
    super.onCreate(state)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    column=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setPadding(32,20,32,20) }
    setContentView(ScrollView(this).apply { addView(column) })
    session=intent.getStringExtra(GameCalibration.EXTRA_SESSION)?.let(store::read)?:store.pending()
    if(session==null) { finish();return }
    launched=state?.getBoolean("launched")?:false
  }
  override fun onSaveInstanceState(out:Bundle) { out.putBoolean("launched",launched);super.onSaveInstanceState(out) }
  override fun onResume() { super.onResume();foreground=true;waitAt=SystemClock.uptimeMillis();handler.post(poll) }
  override fun onPause() { foreground=false;handler.removeCallbacksAndMessages(null);super.onPause() }
  private val poll:Runnable=object:Runnable {
    override fun run() {
      if(!foreground)return
      val s=session?.id?.let(store::read)?:return
      session=s
      val alive=getSystemService(ActivityManager::class.java).runningAppProcesses?.any { it.processName=="$packageName:native" }==true
      if(alive) {
        if(SystemClock.uptimeMillis()-waitAt>10_000) {
          showText(getString(R.string.cal_wait_process));button(R.string.cal_retry) { waitAt=SystemClock.uptimeMillis();handler.post(poll) }
        } else handler.postDelayed(this,50)
        return
      }
      if(s.phase=="running") {
        s.runs=s.runs+CalibrationRun(s.candidates[s.index],null,getString(R.string.cal_crashed));s.index++
        if(s.index==1) { s.error=getString(R.string.cal_crashed);s.phase="result" }
        else s.phase=if(s.index>=s.candidates.size) "result" else "ready"
        store.write(s)
      } else if(s.phase in listOf("applying","returning")) {
        s.phase="restore_failed";s.error=getString(R.string.cal_restore_failed);store.write(s)
      }
      when(s.phase) {
        "ready" -> {
          if(launched || s.index==0) startTrial(s) else showResults(s)
        }
        "result" -> showResults(s)
        "restore_failed" -> {
          showText(getString(R.string.cal_restore_failed)+"\n"+(s.error?:""))
          button(R.string.cal_retry) { restore(s,false) }
          button(R.string.cal_library) { startActivity(Intent(this@CalibrationActivity,LauncherActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish() }
        }
        "complete" -> finish()
        else -> showResults(s)
      }
    }
  }
  private fun startTrial(s:CalibrationSession) {
    if(!store.checkpoint(s.id).isFile) { s.phase="restore_failed";s.error=getString(R.string.cal_no_checkpoint);store.write(s);showResults(s);return }
    if(s.device!=CalibrationStore.deviceKey(this)) { s.error=getString(R.string.cal_device_changed);s.phase="result";store.write(s);showResults(s);return }
    s.phase="running";store.write(s);launched=true
    launchNative(s,false)
  }
  private fun launchNative(s:CalibrationSession,restore:Boolean) {
    startActivity(Intent(this,NativeCoreActivity::class.java)
      .putExtra(NativeCoreActivity.EXTRA_DVD_URI,s.disc)
      .putExtra(NativeCoreActivity.EXTRA_LOADSTATE,store.checkpoint(s.id).absolutePath)
      .putExtra(GameCalibration.EXTRA_SESSION,s.id).putExtra(GameCalibration.EXTRA_RESTORE,restore)
      .addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION),RelaunchActivity.noTransition(this).toBundle())
  }
  private fun restore(s:CalibrationSession,apply:Boolean) {
    if(apply) {
      val winner=CalibrationPolicy.winner(s.runs)?:return
      runCatching { store.publish(s,winner);ExperienceStore(this).set(ExperienceSetting.AUTO_PERFORMANCE,1,s.game) }
        .onFailure { showText(getString(R.string.extra_save_failed));button(R.string.cal_return) { restore(s,false) };return }
    }
    s.phase=if(apply) "applying" else "returning";store.write(s);launchNative(s,true)
  }
  private fun showResults(s:CalibrationSession) {
    val winner=if(s.error==null) CalibrationPolicy.winner(s.runs) else null
    showText(getString(R.string.cal_title)+" — "+s.title+"\n\n"+
      (s.error?:if(winner==null) getString(R.string.cal_inconclusive) else getString(R.string.cal_winner,GameCalibration.describe(winner.options))))
    for((i,r) in s.runs.withIndex()) {
      val m=r.metrics
      val result=if(m?.valid==true) String.format(Locale.getDefault(),
        "%.1f FPS · P95 %.1f ms · CPU %.0f%% · RAM %.0f MB\nGPU %s · CPU/SoC %s · BAT %s",
        m.fps,m.p95Ms,m.cpuMs/(m.seconds*10),m.peakRssMb,
        if(m.gpuBuffers>0) String.format(Locale.getDefault(),"%.2f ms/CB",m.gpuMs/m.gpuBuffers) else getString(R.string.cal_unavailable),
        m.temperatureCpu?.let { "%.1f°C".format(it) }?:"—",m.temperatureBattery?.let { "%.1f°C".format(it) }?:"—")
      else r.error?:getString(R.string.cal_invalid)
      column.addView(TextView(this).apply { text="${i+1}. ${GameCalibration.describe(r.options)}\n$result";textSize=15f;setPadding(0,12,0,12) })
    }
    if(winner!=null) button(R.string.cal_apply) { restore(s,true) }
    button(R.string.cal_return) { restore(s,false) }
  }
  private fun showText(text:String) { column.removeAllViews();column.addView(TextView(this).apply { this.text=text;textSize=19f }) }
  private fun button(label:Int,action:()->Unit) { column.addView(Button(this).apply { text=getString(label);isAllCaps=false;setOnClickListener { action() } }) }
  @Deprecated("Framework back") override fun onBackPressed() { session?.let { restore(it,false) }?:finish() }
  override fun onDestroy() { handler.removeCallbacksAndMessages(null);super.onDestroy() }
}
