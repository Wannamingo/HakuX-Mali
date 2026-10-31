package com.rfandango.haku_x.data

import android.content.Context
import android.os.Build
import android.util.AtomicFile
import com.rfandango.haku_x.GpuDriverHelper
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.UUID

data class CalibrationSession(
  val id: String, val game: String, val title: String, val disc: String, val device: String,
  val original: GraphicsOptions, val candidates: List<GraphicsOptions>, val created: Long,
  var index: Int=0, var phase: String="ready", var runs: List<CalibrationRun> = emptyList(),
  var error: String?=null,
)

/** One foreground process owns a session at a time; native writes before yielding to the coordinator. */
class CalibrationStore(private val context: Context) {
  private val root = File(context.filesDir,"performance")
  fun pending(): CalibrationSession? = runCatching { read(File(root,"pending").readText().trim()) }.getOrNull()
  fun directory(id: String): File { require(id.matches(Regex("[0-9a-f-]{36}"))); return File(root,"sessions/$id") }
  fun checkpoint(id: String) = File(directory(id),"checkpoint.hxs")
  fun thumb(id: String) = File(directory(id),"checkpoint.jpg")
  private fun sessionFile(id: String) = AtomicFile(File(directory(id),"session.json"))
  fun create(game: String,title: String,disc: String, original: GraphicsOptions, ceiling: Int): CalibrationSession {
    val s=CalibrationSession(UUID.randomUUID().toString(),game,title,disc,configurationKey(game),original,
      CalibrationPolicy.candidates(original,ceiling),System.currentTimeMillis())
    require(pending()?.phase in listOf(null,"complete","discarded"))
    directory(s.id).mkdirs();write(s);File(root,"pending").writeText(s.id);return s
  }
  fun write(s: CalibrationSession) {
    val json=JSONObject().put("schema",CalibrationPolicy.SCHEMA).put("id",s.id).put("game",s.game)
      .put("title",s.title).put("disc",s.disc).put("device",s.device).put("created",s.created)
      .put("original",graphics(s.original)).put("candidates",JSONArray(s.candidates.map(::graphics)))
      .put("index",s.index).put("phase",s.phase).put("error",s.error)
      .put("runs",JSONArray(s.runs.map { r -> JSONObject().put("options",graphics(r.options))
        .put("metrics",r.metrics?.let(::metrics)).put("error",r.error) }))
    atomicWrite(sessionFile(s.id),json)
  }
  fun read(id: String): CalibrationSession? = runCatching {
    val j=sessionFile(id).openRead().bufferedReader().use { JSONObject(it.readText()) }
    require(j.getInt("schema")==CalibrationPolicy.SCHEMA)
    require(j.getString("id")==id)
    val choices=j.getJSONArray("candidates");require(choices.length() in 2..6)
    val runs=j.getJSONArray("runs");require(runs.length()<=choices.length())
    CalibrationSession(id,j.getString("game"),j.getString("title"),j.getString("disc"),j.getString("device"),
      graphics(j.getJSONObject("original")),(0 until choices.length()).map { graphics(choices.getJSONObject(it)) },
      j.getLong("created"),j.getInt("index"),j.getString("phase"),
      (0 until runs.length()).map { k -> runs.getJSONObject(k).let { r ->
        CalibrationRun(graphics(r.getJSONObject("options")),r.optJSONObject("metrics")?.let(::metrics),
          r.optString("error").takeIf { it.isNotEmpty() && it!="null" }) } },
      j.optString("error").takeIf { it.isNotEmpty() && it!="null" })
      .also { require(it.index in 0..it.candidates.size) }
  }.getOrNull()

  private fun profileFile(game:String) = AtomicFile(File(root,"profiles/${digest(game)}.json"))
  fun measured(game: String): CalibrationRun? = runCatching {
    val j=profileFile(game).openRead().bufferedReader().use { JSONObject(it.readText()) }
    require(j.getString("device")==configurationKey(game) && j.getInt("schema")==CalibrationPolicy.SCHEMA)
    CalibrationRun(graphics(j.getJSONObject("options")),metrics(j.getJSONObject("metrics"))).also { require(it.metrics!!.valid) }
  }.getOrNull()
  fun publish(s: CalibrationSession, winner: CalibrationRun) {
    require(s.device==configurationKey(s.game) && winner.metrics?.valid==true)
    atomicWrite(profileFile(s.game),JSONObject().put("schema",CalibrationPolicy.SCHEMA).put("device",s.device)
      .put("game",s.game).put("measuredAt",System.currentTimeMillis()).put("options",graphics(winner.options))
      .put("metrics",metrics(winner.metrics!!)).put("session",s.id))
  }
  fun cleanupCheckpoint(id: String) {
    val dir=directory(id)
    // Only this session's private checkpoint and content-addressed blobs, after successful restore.
    checkpoint(id).delete();thumb(id).delete();File(dir,"blobs").deleteRecursively()
    if (runCatching { File(root,"pending").readText().trim()==id }.getOrDefault(false)) File(root,"pending").delete()
  }
  fun configurationKey(game:String):String {
    val options=ExperienceStore(context).effective(game)
    return digest(deviceKey(context)+"|aniso="+options[ExperienceSetting.ANISOTROPY]+"|movie="+options[ExperienceSetting.MOVIE_ASPECT]+
      "|"+options.dynamicArgument())
  }
  companion object {
    fun active(context:Context,game:String) {
      val file=File(context.filesDir,"performance/active.json");file.parentFile?.mkdirs()
      file.writeText(JSONObject().put("game",game).put("pid",android.os.Process.myPid()).toString())
    }
    fun activeGame(context:Context):String?=runCatching {
      val j=JSONObject(File(context.filesDir,"performance/active.json").readText())
      val alive=context.getSystemService(android.app.ActivityManager::class.java).runningAppProcesses?.any {
        it.pid==j.getInt("pid") && it.processName=="${context.packageName}:native"
      }==true
      j.getString("game").takeIf { alive }
    }.getOrNull()
    fun digest(value: String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    fun deviceKey(context: Context): String {
      GpuDriverHelper.init(context)
      val driver=GpuDriverHelper.getActiveDriver()
      val library=driver?.libraryName?.let { File(GpuDriverHelper.driverInstallDir,it) }
      @Suppress("DEPRECATION") val version=context.packageManager.getPackageInfo(context.packageName,0).versionCode
      return digest(listOf(Build.FINGERPRINT,Build.MODEL,Build.HARDWARE,
        if (Build.VERSION.SDK_INT>=31) Build.SOC_MODEL else "",version,driver?.name,driver?.driverVersion,
        library?.length(),library?.lastModified()).joinToString("|"))
    }
    private fun graphics(v: GraphicsOptions)=JSONObject().put("scale",v.scale).put("fxaa",v.fxaa).put("upscale",v.upscale).put("fps",v.fpsLimit)
    private fun graphics(j: JSONObject)=GraphicsOptions(j.getInt("scale"),j.getBoolean("fxaa"),j.getString("upscale"),j.getInt("fps"))
      .also { require(it.scale in 1..4 && it.upscale in listOf("bilinear","sgsr") && it.fpsLimit in 0..240) }
    private fun metrics(v: CalibrationMetrics)=JSONObject().put("seconds",v.seconds).put("frames",v.frames).put("samples",v.samples)
      .put("median",v.medianMs).put("p95",v.p95Ms).put("max",v.maxMs).put("cpuMs",v.cpuMs).put("rssMb",v.peakRssMb)
      .put("gpuMs",v.gpuMs).put("gpuBuffers",v.gpuBuffers).put("cpuTemp",v.temperatureCpu)
      .put("gpuTemp",v.temperatureGpu).put("batteryTemp",v.temperatureBattery)
    private fun metrics(j: JSONObject)=CalibrationMetrics(j.getDouble("seconds"),j.getLong("frames"),j.getInt("samples"),
      j.getDouble("median"),j.getDouble("p95"),j.getDouble("max"),j.getDouble("cpuMs"),j.getDouble("rssMb"),
      j.optDouble("gpuMs",-1.0),j.optLong("gpuBuffers",0),j.optionalDouble("cpuTemp"),j.optionalDouble("gpuTemp"),j.optionalDouble("batteryTemp"))
    private fun JSONObject.optionalDouble(key:String)=if (has(key)&&!isNull(key)) optDouble(key).takeIf { it.isFinite() } else null
    private fun atomicWrite(file:AtomicFile,json:JSONObject) {
      file.baseFile.parentFile?.mkdirs();val output=file.startWrite()
      try { output.write(json.toString().toByteArray());file.finishWrite(output) }
      catch(e:Exception){file.failWrite(output);throw e}
    }
  }
}
