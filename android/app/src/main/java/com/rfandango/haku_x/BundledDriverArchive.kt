package com.rfandango.haku_x

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/** Verify before replacing the private copy; a failed asset read leaves the old package intact. */
object BundledDriverArchive {
  fun sha256(input: InputStream): String {
    val hash = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(64 * 1024)
    while (true) {
      val n = input.read(buffer)
      if (n < 0) break
      hash.update(buffer, 0, n)
    }
    return hash.digest().joinToString("") { "%02x".format(it) }
  }

  fun sha256(file: File): String = file.inputStream().use(::sha256)

  fun ensure(target: File, expected: String, open: () -> InputStream) {
    if (target.isFile && sha256(target) == expected) return
    target.parentFile?.mkdirs()
    val staged = File(target.parentFile, target.name + ".new")
    try {
      open().use { input -> staged.outputStream().use { input.copyTo(it) } }
      if (sha256(staged) != expected) throw IOException("Bundled MaliSX2 checksum mismatch")
      try {
        Files.move(staged.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
      } catch (_: AtomicMoveNotSupportedException) {
        Files.move(staged.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
      }
    } finally {
      staged.delete()
    }
  }
}
