package com.rfandango.haku_x

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

/** Package requirements for this arm64 app, independent of the device model and GPU vendor. */
object DriverPackageArchive {
  enum class Error { UNREADABLE, NO_LIBRARY, API, ABI, UNSAFE_PATH }

  fun check(file: File, library: String?, minApi: Int, sdk: Int): Error? {
    if (!GpuDriverPolicy.acceptsLibrary(library)) return Error.NO_LIBRARY
    if (!GpuDriverPolicy.canLoadPackage(sdk, minApi)) return Error.API
    return try {
      ZipFile(file).use { zip ->
        if (zip.entries().asSequence().any { unsafePath(it.name) }) return Error.UNSAFE_PATH
        val entry = zip.getEntry(library) ?: return Error.NO_LIBRARY
        if (entry.isDirectory) return Error.NO_LIBRARY
        if (zip.getInputStream(entry).use { arm64SharedLibrary(header(it)) }) null else Error.ABI
      }
    } catch (_: Exception) { Error.UNREADABLE }
  }

  private fun unsafePath(name: String): Boolean {
    val path = name.replace('\\', '/')
    return path.startsWith('/') || ':' in path || '\u0000' in path || path.split('/').any { it == ".." }
  }

  private fun header(input: InputStream): ByteArray {
    val bytes = ByteArray(64)
    var count = 0
    while (count < bytes.size) {
      val n = input.read(bytes, count, bytes.size - count)
      if (n < 0) break
      if (n == 0) continue
      count += n
    }
    return bytes.copyOf(count)
  }

  internal fun arm64SharedLibrary(header: ByteArray): Boolean = header.size >= 64 &&
    header[0] == 0x7f.toByte() && header[1] == 'E'.code.toByte() &&
    header[2] == 'L'.code.toByte() && header[3] == 'F'.code.toByte() &&
    header[4] == 2.toByte() && header[5] == 1.toByte() && header[6] == 1.toByte() &&
    header[16] == 3.toByte() && header[17] == 0.toByte() &&
    header[18] == 183.toByte() && header[19] == 0.toByte()
}
