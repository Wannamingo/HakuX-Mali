package com.rfandango.haku_x.data

import android.content.Context
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File

data class RendererCapabilities(val maxAnisotropy:Int) {
  val anisotropyChoices get() = listOf(0)+listOf(2,4,8,16).filter { it<=maxAnisotropy }
  companion object { fun bounded(value:Int)=RendererCapabilities(value.coerceIn(1,16)) }
}

/** Capabilities of the Vulkan device actually loaded, not a guess from its GPU
 * name. Invalidated when the build, system or selected driver changes. */
class RendererCapabilitiesStore(private val context:Context) {
  private val file=AtomicFile(File(context.filesDir,"renderer-capabilities.json"))
  fun read():RendererCapabilities?=runCatching {
    val j=file.openRead().bufferedReader().use { JSONObject(it.readText()) }
    require(j.getString("device")==CalibrationStore.deviceKey(context))
    RendererCapabilities.bounded(j.getInt("maxAnisotropy"))
  }.getOrNull()
  fun write(nativeJson:String) {
    val j=JSONObject(nativeJson)
    require(j.getInt("maxAnisotropy") in 1..1024)
    j.put("device",CalibrationStore.deviceKey(context))
    val output=file.startWrite()
    try { output.write(j.toString().toByteArray());file.finishWrite(output) }
    catch(e:Exception) { file.failWrite(output);throw e }
  }
}
