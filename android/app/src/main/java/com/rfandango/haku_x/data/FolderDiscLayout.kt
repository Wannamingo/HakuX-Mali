package com.rfandango.haku_x.data

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale

/** A read-only XDVDFS view: only directory tables live in memory, file bytes stay in their sources. */
class FolderDiscLayout(files: List<Source>) {
  data class Source(val path: String, val uri: String, val size: Long)
  data class Extent(val start: Long, val size: Long, val bytes: ByteArray? = null, val source: Source? = null)
  private class Node(val name: String, val source: Source? = null) {
    val children = linkedMapOf<String, Node>()
    val directory get() = source == null
    val nameBytes = name.toByteArray(Charsets.ISO_8859_1)
    var sector = 0L
    var table = 0
    var offset = 0
    var left: Node? = null
    var right: Node? = null
    var order: List<Node> = emptyList()
  }
  val extents: List<Extent>
  val size: Long

  init {
    require(files.size <= 100_000) { "Too many files in the game folder" }
    val root = Node("")
    for (f in files) {
      require(f.size in 0..0xFFFFFFFFL) { "File too large for Xbox: ${f.path}" }
      val path = XboxGamePath.parts(f.path)
      require(path.isNotEmpty()) { "Empty game path" }
      var parent = root
      for ((i, name) in path.withIndex()) {
        require(name.all { it.code in 1..255 } && name.toByteArray(Charsets.ISO_8859_1).size <= 255) { "Invalid Xbox filename: $name" }
        val key = name.lowercase(Locale.ROOT)
        val last = i == path.lastIndex
        if (last) {
          require(key !in parent.children) { "Duplicate Xbox path: ${f.path}" }
          parent.children[key] = Node(name, f)
        } else {
          parent = parent.children.getOrPut(key) { Node(name) }
          require(parent.directory) { "A file is also a folder: ${f.path}" }
        }
      }
    }
    require(root.children["default.xbe"]?.source != null) { "The game folder has no default.xbe" }
    fun tables(dir: Node) {
      val sorted = dir.children.values.sortedWith { a, b -> compareNames(a.nameBytes, b.nameBytes) }
      val order = ArrayList<Node>()
      fun tree(lo: Int, hi: Int): Node? {
        if (lo > hi) return null
        val mid = (lo + hi) / 2
        return sorted[mid].also { n ->
          order += n
          n.left = tree(lo, mid - 1)
          n.right = tree(mid + 1, hi)
        }
      }
      tree(0, sorted.lastIndex)
      dir.order = order
      var at = 0
      for (n in order) {
        val len = (14 + n.nameBytes.size + 3) and 3.inv()
        if (at / SECTOR != (at + len - 1) / SECTOR) at = roundSector(at.toLong()).toInt()
        require(at / 4 < 0xFFFF) { "Too many entries in Xbox directory ${dir.name}" }
        n.offset = at
        at += len
      }
      dir.table = roundSector(maxOf(at, 1).toLong()).toInt()
      require(dir.table <= 256 * 1024) { "Xbox directory too large: ${dir.name}" }
      for (n in order) if (n.directory) tables(n)
    }
    tables(root)
    var sector = 0x108L
    fun place(dir: Node) {
      dir.sector = sector
      sector += dir.table / SECTOR
      for (n in dir.order) if (!n.directory) {
        n.sector = sector
        sector += roundSector(n.source!!.size) / SECTOR
      }
      for (n in dir.order) if (n.directory) place(n)
    }
    place(root)
    require(sector <= 0xFFFFFFFFL) { "Game folder is too large" }
    size = ((sector * SECTOR + 65535) / 65536) * 65536
    val ranges = ArrayList<Extent>()
    val vd = ByteBuffer.allocate(SECTOR).order(ByteOrder.LITTLE_ENDIAN)
    val magic = "MICROSOFT*XBOX*MEDIA".toByteArray(Charsets.US_ASCII)
    vd.put(magic).putInt(root.sector.toInt()).putInt(root.table).putLong(0)
    vd.position(SECTOR - magic.size); vd.put(magic)
    ranges += Extent(0x10000, SECTOR.toLong(), bytes = vd.array())
    fun emit(dir: Node) {
      val b = ByteBuffer.wrap(ByteArray(dir.table) { 0xFF.toByte() }).order(ByteOrder.LITTLE_ENDIAN)
      for (n in dir.order) {
        b.position(n.offset)
        b.putShort(((n.left?.offset ?: 0) / 4).toShort())
        b.putShort(((n.right?.offset ?: 0) / 4).toShort())
        b.putInt(n.sector.toInt())
        b.putInt(if (n.directory) n.table else n.source!!.size.toInt())
        b.put(if (n.directory) 0x10.toByte() else 0x20.toByte())
        b.put(n.nameBytes.size.toByte()).put(n.nameBytes)
      }
      ranges += Extent(dir.sector * SECTOR, dir.table.toLong(), bytes = b.array())
      for (n in dir.order) {
        if (n.directory) emit(n)
        else if (n.source!!.size > 0) ranges += Extent(n.sector * SECTOR, n.source.size, source = n.source)
      }
    }
    emit(root)
    extents = ranges.sortedBy { it.start }
  }

  /** Fills arbitrary sector/file-crossing reads without materializing the image or a whole file. */
  fun read(offset: Long, requested: Int, out: ByteArray,
           sourceRead: (Source, Long, ByteArray, Int, Int) -> Int): Int {
    require(offset >= 0 && requested >= 0 && requested <= out.size)
    val count = minOf(requested.toLong(), (size - offset).coerceAtLeast(0)).toInt()
    out.fill(0, 0, count)
    if (count == 0) return 0
    var lo = 0; var hi = extents.size
    while (lo < hi) {
      val m = (lo + hi) ushr 1
      if (extents[m].start + extents[m].size <= offset) lo = m + 1 else hi = m
    }
    var i = lo
    while (i < extents.size) {
      val e = extents[i++]
      if (e.start >= offset + count) break
      val from = maxOf(offset, e.start)
      val n = (minOf(offset + count, e.start + e.size) - from).toInt()
      val dst = (from - offset).toInt()
      if (e.bytes != null) e.bytes.copyInto(out, dst, (from - e.start).toInt(), (from - e.start).toInt() + n)
      else {
        var done = 0
        while (done < n) {
          val got = sourceRead(e.source!!, from - e.start + done, out, dst + done, n - done)
          if (got <= 0 || got > n - done) throw IOException("The game file changed or cannot be read: ${e.source.path}")
          done += got
        }
      }
    }
    return count
  }

  companion object {
    private const val SECTOR = 2048
    private fun roundSector(n: Long) = (n + SECTOR - 1) / SECTOR * SECTOR
    private fun compareNames(a: ByteArray, b: ByteArray): Int {
      fun upper(c: Byte): Int = (c.toInt() and 255).let { if (it in 97..122) it - 32 else it }
      for (i in 0 until minOf(a.size, b.size)) {
        val c = upper(a[i]).compareTo(upper(b[i])); if (c != 0) return c
      }
      return a.size.compareTo(b.size)
    }
  }
}

/** Xbox paths are relative, bounded and never interpret Windows/Unix traversal or drive names. */
object XboxGamePath {
  fun parts(raw: String): List<String> {
    require(raw.isNotEmpty() && raw.length <= 4096 && !raw.startsWith('/') && !raw.startsWith('\\')) { "Invalid game path: $raw" }
    val path = raw.replace('\\', '/').trimEnd('/')
    val parts = path.split('/')
    require(parts.size <= 64 && parts.all { it.isNotEmpty() && it != "." && it != ".." && ':' !in it && '\u0000' !in it }) { "Unsafe game path: $raw" }
    return parts
  }
}
