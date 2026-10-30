package com.rfandango.haku_x.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.AtomicFile
import androidx.core.content.edit
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** UI-only preferences and bounded, app-owned cover copies; never writes to game folders. */
class LibraryPresentation(private val context: Context) {
  private val prefs = context.getSharedPreferences("library_ui", Context.MODE_PRIVATE)
  var covers: Boolean
    get() = prefs.getBoolean("covers", true)
    set(value) { prefs.edit { putBoolean("covers", value) } }
  var coverSize: Int
    get() = prefs.getInt("cover_size", 1).coerceIn(0, 2)
    set(value) { prefs.edit { putInt("cover_size", value.coerceIn(0, 2)) } }
  val recent: List<String> get() = runCatching {
    val array = JSONArray(prefs.getString("recent", "[]"))
    (0 until minOf(array.length(), 12)).map { array.getString(it) }
  }.getOrDefault(emptyList())
  fun record(uri: String) { prefs.edit { putString("recent", JSONArray(RecentGames.record(recent, uri)).toString()) } }

  fun coverFile(game: Game, disc: DiscInfo?): File {
    val key = if (disc != null && disc.titleId != 0L) "${disc.titleId}.${disc.version}" else game.uri.toString()
    val name = MessageDigest.getInstance("SHA-256").digest(key.toByteArray()).joinToString("") { "%02x".format(it) }
    return File(context.filesDir, "covers/$name.jpg")
  }

  fun importCover(game: Game, disc: DiscInfo?, uri: Uri) {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth !in 1..16384 || bounds.outHeight !in 1..16384 || bounds.outWidth.toLong() * bounds.outHeight > 100_000_000)
      throw IOException("Invalid image dimensions")
    val options = BitmapFactory.Options().apply { inSampleSize = 1 }
    while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize.coerceAtLeast(1) > 768)
      options.inSampleSize = options.inSampleSize.coerceAtLeast(1) * 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
      ?: throw IOException("Unreadable cover")
    try {
      val file = coverFile(game, disc)
      file.parentFile!!.mkdirs()
      val atomic = AtomicFile(file)
      val output = atomic.startWrite()
      try {
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw IOException("Could not store cover")
        atomic.finishWrite(output)
      } catch (e: Exception) { atomic.failWrite(output); throw e }
    } finally { bitmap.recycle() }
  }
}
