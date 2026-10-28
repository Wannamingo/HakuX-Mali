package com.rfandango.haku_x

import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BundledDriverArchiveTest {
  @get:Rule val temp = TemporaryFolder()
  private fun hash(bytes: ByteArray) = BundledDriverArchive.sha256(ByteArrayInputStream(bytes))

  @Test fun freshCopyAndVerifiedCopyReuse() {
    val target = File(temp.root, "driver.zip")
    val bytes = byteArrayOf(0, 1, 2, 127, -1)
    BundledDriverArchive.ensure(target, hash(bytes)) { ByteArrayInputStream(bytes) }
    assertArrayEquals(bytes, target.readBytes())
    BundledDriverArchive.ensure(target, hash(bytes)) { error("A verified copy must not reopen the asset") }
  }

  @Test fun failedChecksumKeepsPreviousPackage() {
    val target = temp.newFile("driver.zip").apply { writeText("previous") }
    try {
      BundledDriverArchive.ensure(target, hash("expected".toByteArray())) { ByteArrayInputStream("corrupt".toByteArray()) }
      fail("Corrupted asset accepted")
    } catch (_: IOException) { }
    assertEquals("previous", target.readText())
    assertFalse(File(temp.root, "driver.zip.new").exists())
  }

  @Test fun updateReplacesOldPackageAfterVerification() {
    val target = temp.newFile("driver.zip").apply { writeText("old") }
    val bytes = "updated driver".toByteArray()
    BundledDriverArchive.ensure(target, hash(bytes)) { ByteArrayInputStream(bytes) }
    assertArrayEquals(bytes, target.readBytes())
    assertFalse(File(temp.root, "driver.zip.new").exists())
  }
}
