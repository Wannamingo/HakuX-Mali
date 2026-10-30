package com.rfandango.haku_x.data

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.system.ErrnoException
import android.system.OsConstants
import android.util.AtomicFile
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.security.MessageDigest

/** Folder indexing happens before launch, not on the UI/render thread. Native code still reads a seekable disc FD. */
object FolderDisc {
  fun key(uri: Uri): String = MessageDigest.getInstance("SHA-256").digest(uri.toString().toByteArray()).joinToString("") { "%02x".format(it) }
  private fun index(context: Context, uri: Uri) = File(context.filesDir, "game_sources/${key(uri)}.folder.json")

  fun prepare(context: Context, uri: Uri, cancelled: () -> Boolean, progress: (Int) -> Unit) {
    val files = ArrayList<FolderDiscLayout.Source>()
    val seen = HashSet<String>()
    fun visit(dir: Uri, path: String, depth: Int) {
      if (cancelled()) throw java.io.InterruptedIOException("Game preparation cancelled")
      require(depth <= 64 && seen.add(dir.toString())) { "Game folder tree is too deep or cyclic" }
      for (entry in GameDocuments.children(context, dir)) {
        if (cancelled()) throw java.io.InterruptedIOException("Game preparation cancelled")
        val name = if (path.isEmpty()) entry.name else "$path/${entry.name}"
        if (entry.directory) visit(entry.uri, name, depth + 1) else {
          require(files.size < 100_000) { "Too many game files" }
          files += FolderDiscLayout.Source(name, entry.uri.toString(), entry.size)
          if (files.size % 64 == 0) progress(files.size)
        }
      }
    }
    visit(uri, "", 0)
    val layout = FolderDiscLayout(files) // validate Xbox paths and table limits before publishing the index
    val list = JSONArray()
    files.forEach { list.put(JSONObject().put("path", it.path).put("uri", it.uri).put("size", it.size)) }
    val json = JSONObject().put("version", 1).put("source", uri.toString()).put("files", list)
    write(index(context, uri), json)
    progress(files.size)
    Log.i("hakuX-source", "Folder ready: ${files.size} files, ${layout.size} virtual bytes; no ISO copy")
  }

  internal fun write(file: File, json: JSONObject) {
    file.parentFile!!.mkdirs()
    val atomic = AtomicFile(file)
    val out = atomic.startWrite()
    try { out.write(json.toString().toByteArray()); atomic.finishWrite(out) }
    catch (e: Exception) { atomic.failWrite(out); throw e }
  }

  fun open(context: Context, uri: Uri): ParcelFileDescriptor {
    val json = JSONObject(index(context, uri).readText())
    require(json.getInt("version") == 1 && json.getString("source") == uri.toString()) { "Invalid folder index" }
    val a = json.getJSONArray("files")
    val sources = (0 until a.length()).map { i -> a.getJSONObject(i).let { FolderDiscLayout.Source(it.getString("path"), it.getString("uri"), it.getLong("size")) } }
    val layout = FolderDiscLayout(sources)
    val app = context.applicationContext
    val thread = HandlerThread("hakuX-folder-io").apply { start() }
    val readers = object : LinkedHashMap<String, FileInputStream>(32, .75f, true) {}
    val callback = object : ProxyFileDescriptorCallback() {
      override fun onGetSize(): Long = layout.size
      override fun onRead(offset: Long, size: Int, data: ByteArray): Int = try {
        layout.read(offset, size, data) { file, at, target, start, count ->
          val reader = readers[file.uri] ?: run {
            val fd = app.contentResolver.openFileDescriptor(Uri.parse(file.uri), "r") ?: throw IOException("Can't read ${file.path}")
            val stream = ParcelFileDescriptor.AutoCloseInputStream(fd)
            if (readers.size >= 24) {
              val first = readers.entries.iterator().next()
              first.value.close(); readers.remove(first.key)
            }
            readers[file.uri] = stream
            stream
          }
          reader.channel.read(ByteBuffer.wrap(target, start, count), at)
        }
      } catch (e: Exception) {
        Log.e("hakuX-source", "Folder read failed at $offset", e)
        throw ErrnoException("folder read", OsConstants.EIO, e)
      }
      override fun onRelease() {
        readers.values.forEach { runCatching { it.close() } }; readers.clear()
        thread.quitSafely()
      }
    }
    return try {
      app.getSystemService(StorageManager::class.java).openProxyFileDescriptor(ParcelFileDescriptor.MODE_READ_ONLY, callback, Handler(thread.looper))
    } catch (e: Exception) { thread.quitSafely(); throw e }
  }
}
