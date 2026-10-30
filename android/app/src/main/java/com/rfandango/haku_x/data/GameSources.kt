package com.rfandango.haku_x.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.util.Locale

/** Images and Xbox folders share the same launch/relaunch/savestate entry. No game data is copied. */
object GameSources {
  val imageExtensions = setOf("iso", "xiso", "cso", "cci")
  data class Progress(val done: Long = 0)
  data class Choice(val label: String, val key: String)
  sealed interface Result {
    data class Ready(val uri: Uri) : Result
    data class Choose(val choices: List<Choice>) : Result
  }

  private fun redirect(context: Context, uri: Uri) = File(context.filesDir, "game_sources/${FolderDisc.key(uri)}.source.json")
  fun resolve(context: Context, uri: Uri): Uri = runCatching {
    val json = JSONObject(redirect(context, uri).readText())
    require(json.getString("source") == uri.toString())
    val target = Uri.parse(json.getString("target"))
    if (target.scheme == "file" && !File(target.path.orEmpty()).exists()) uri else target
  }.getOrDefault(uri)

  fun prepare(context: Context, original: Uri, selection: String? = null,
              progress: (Progress) -> Unit, cancelled: () -> Boolean): Result {
    fun check() { if (cancelled()) throw InterruptedIOException("Game preparation cancelled") }
    check()
    var doc = GameDocuments.stat(context, original)
    if (!doc.directory && doc.name.equals("default.xbe", true)) {
      val parent = if (original.scheme == "file") Uri.fromFile(File(original.path!!).parentFile!!) else {
        val id = DocumentsContract.getDocumentId(original)
        val parentId = id.substringBeforeLast('/', id.substringBefore(':') + ":")
        runCatching { DocumentsContract.buildDocumentUriUsingTree(original, parentId) }.getOrElse {
          throw IOException("Select the folder containing default.xbe, so the game's other files can be read")
        }
      }
      doc = GameDocuments.stat(context, parent)
    }
    if (!doc.directory) {
      val ext = doc.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
      if (ext !in imageExtensions) throw IOException("Select an Xbox image or a folder containing default.xbe")
      return Result.Ready(original)
    }
    progress(Progress())
    val candidates = ArrayList<GameDocuments.Doc>()
    val labels = HashMap<String, String>()
    var visited = 0
    val seen = HashSet<String>()
    fun find(dir: GameDocuments.Doc, path: String, depth: Int) {
      check()
      require(depth <= 64 && visited++ < 100_000 && seen.add(dir.uri.toString())) { "Game folder tree is too large or cyclic" }
      val children = GameDocuments.children(context, dir.uri)
      if (children.any { !it.directory && it.name.equals("default.xbe", true) }) {
        candidates += dir; labels[dir.uri.toString()] = path.ifEmpty { dir.name }; return
      }
      for (child in children) {
        val name = if (path.isEmpty()) child.name else "$path/${child.name}"
        if (child.directory) find(child, name, depth + 1)
        else if (child.name.substringAfterLast('.', "").lowercase(Locale.ROOT) in imageExtensions) {
          candidates += child; labels[child.uri.toString()] = name
        }
      }
    }
    find(doc, "", 0)
    if (candidates.isEmpty()) throw IOException("No Xbox image or folder with default.xbe was found")
    val chosen = candidates.firstOrNull { it.uri.toString() == selection } ?: candidates.singleOrNull()
    if (chosen == null) return Result.Choose(candidates.map { Choice(labels.getValue(it.uri.toString()), it.uri.toString()) }.sortedBy { it.label })
    if (chosen.directory) FolderDisc.prepare(context, chosen.uri, cancelled) { progress(Progress(it.toLong())) }
    check()
    FolderDisc.write(redirect(context, original), JSONObject().put("source", original.toString()).put("target", chosen.uri.toString()))
    return Result.Ready(original)
  }

  /** Library metadata reads the folder's XBE directly. */
  fun metadata(context: Context, uri: Uri): Pair<Uri, Boolean>? {
    val target = resolve(context, uri)
    val doc = GameDocuments.stat(context, target)
    if (doc.directory) return GameDocuments.children(context, target)
      .firstOrNull { !it.directory && it.name.equals("default.xbe", true) }?.let { it.uri to true }
    if (doc.name.equals("default.xbe", true)) return target to true
    return if (doc.name.substringAfterLast('.', "").lowercase(Locale.ROOT) in imageExtensions) target to false else null
  }

  fun open(context: Context, uri: Uri): ParcelFileDescriptor {
    val target = resolve(context, uri)
    val doc = GameDocuments.stat(context, target)
    return if (doc.directory) FolderDisc.open(context, target) else
      context.contentResolver.openFileDescriptor(target, "r") ?: throw IOException("Can't open the game")
  }
}
