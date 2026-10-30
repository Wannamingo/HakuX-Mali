package com.rfandango.haku_x.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException

/** Single-query SAF listings and metadata for image files supplied by other apps. */
object GameDocuments {
  data class Doc(val uri: Uri, val name: String, val directory: Boolean, val size: Long, val modified: Long)
  private val columns = arrayOf("document_id", "_display_name", "mime_type", "_size", "last_modified")
  /** Frontends commonly pass an absolute SD path; reuse its existing SAF grant instead of requiring broad storage access. */
  fun fromPath(context: Context, path: String): Uri {
    val normalized = File(path).toPath().normalize().toString().replace('\\', '/')
    for (grant in context.contentResolver.persistedUriPermissions) {
      if (!grant.isReadPermission || grant.uri.authority != "com.android.externalstorage.documents") continue
      val id = runCatching { DocumentsContract.getTreeDocumentId(grant.uri) }.getOrNull() ?: continue
      val volume = id.substringBefore(':'); val base = id.substringAfter(':').trim('/')
      val prefixes = if (volume == "primary") listOf("/storage/emulated/0/", "/sdcard/", "/storage/self/primary/") else listOf("/storage/$volume/", "/mnt/media_rw/$volume/")
      for (prefix in prefixes) {
        if (!normalized.startsWith(prefix, true)) continue
        val relative = normalized.substring(prefix.length)
        if (base.isEmpty() || relative == base || relative.startsWith("$base/")) {
          return DocumentsContract.buildDocumentUriUsingTree(grant.uri, "$volume:$relative")
        }
      }
    }
    return Uri.fromFile(File(path))
  }
  fun stat(context: Context, uri: Uri): Doc {
    if (uri.scheme == "file") {
      val f = File(uri.path ?: throw IOException("Missing game path"))
      if (!f.exists()) throw IOException("Game file or folder is unavailable")
      return Doc(uri, f.name, f.isDirectory, if (f.isFile) f.length() else 0, f.lastModified())
    }
    if (!DocumentsContract.isDocumentUri(context, uri) && !DocumentsContract.isTreeUri(uri)) {
      context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst()) return Doc(uri, c.getString(0).orEmpty(), false, if (c.isNull(1)) 0 else c.getLong(1), 0)
      }
      throw IOException("Game file metadata is unavailable")
    }
    val document = if (DocumentsContract.isDocumentUri(context, uri)) uri else
      DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
    context.contentResolver.query(document, columns, null, null, null)?.use { c ->
      if (c.moveToFirst()) return Doc(document, c.getString(1).orEmpty(), c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR,
        if (c.isNull(3)) 0 else c.getLong(3), if (c.isNull(4)) 0 else c.getLong(4))
    }
    throw IOException("Game file or folder is unavailable")
  }
  fun children(context: Context, uri: Uri): List<Doc> {
    if (uri.scheme == "file") {
      val dir = File(uri.path ?: throw IOException("Missing game folder"))
      return (dir.listFiles() ?: throw IOException("Can't read game folder")).map {
        Doc(Uri.fromFile(it), it.name, it.isDirectory, if (it.isFile) it.length() else 0, it.lastModified())
      }
    }
    val id = if (DocumentsContract.isDocumentUri(context, uri)) DocumentsContract.getDocumentId(uri) else DocumentsContract.getTreeDocumentId(uri)
    val out = ArrayList<Doc>()
    val children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, id)
    context.contentResolver.query(children, columns, null, null, null)?.use { c ->
      while (c.moveToNext()) {
        val child = c.getString(0) ?: continue
        out += Doc(DocumentsContract.buildDocumentUriUsingTree(uri, child), c.getString(1).orEmpty(),
          c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR, if (c.isNull(3)) 0 else c.getLong(3), if (c.isNull(4)) 0 else c.getLong(4))
      }
      return out
    }
    throw IOException("Can't read game folder")
  }
}
