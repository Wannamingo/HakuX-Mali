package com.rfandango.haku_x

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DriverPackageArchiveTest {
  @get:Rule val temporary = TemporaryFolder()

  private fun elf(machine: Int = 183, elfClass: Int = 2): ByteArray = ByteArray(64).apply {
    this[0]=0x7f; this[1]='E'.code.toByte(); this[2]='L'.code.toByte(); this[3]='F'.code.toByte()
    this[4]=elfClass.toByte(); this[5]=1; this[6]=1; this[16]=3; this[18]=machine.toByte()
  }
  private fun zip(vararg entries: Pair<String,ByteArray>): File = temporary.newFile().apply {
    ZipOutputStream(outputStream()).use { zip ->
      for ((name,data) in entries) {
        zip.putNextEntry(ZipEntry(name));zip.write(data);zip.closeEntry()
      }
    }
  }
  private fun check(file: File, library: String = "driver.so", minApi: Int = 28, sdk: Int = 34) =
    DriverPackageArchive.check(file, library, minApi, sdk)

  @Test fun arbitraryDriverVendorsUseTheSameArchitectureChecks() {
    for (name in listOf("libvulkan_malisx2.so","libvulkan_freedreno.so","custom.so"))
      assertNull(check(zip(name to elf()),name))
  }
  @Test fun actualAndroidMinimumRemainsMandatory() {
    val file=zip("driver.so" to elf())
    assertEquals(DriverPackageArchive.Error.API,check(file,minApi=35,sdk=34))
    assertNull(check(file,minApi=34,sdk=34))
    assertEquals(DriverPackageArchive.Error.API,check(file,minApi=-1))
  }
  @Test fun wrongArchitectureAndTruncatedLibrariesAreRejected() {
    assertEquals(DriverPackageArchive.Error.ABI,check(zip("driver.so" to elf(62))))
    assertEquals(DriverPackageArchive.Error.ABI,check(zip("driver.so" to elf(40,1))))
    assertEquals(DriverPackageArchive.Error.ABI,check(zip("driver.so" to elf().copyOf(20))))
    assertEquals(DriverPackageArchive.Error.ABI,check(zip("driver.so" to byteArrayOf())))
  }
  @Test fun executablesAndBigEndianOrNonElfDataAreNotArm64DriverLibraries() {
    for (bytes in listOf(elf().apply{this[16]=2},elf().apply{this[5]=2},elf().apply{this[0]=0}))
      assertFalse(DriverPackageArchive.arm64SharedLibrary(bytes))
  }
  @Test fun anyTraversalOrAbsoluteEntryRejectsTheWholePackage() {
    for (path in listOf("../escape","licenses/../../escape","/absolute","..\\escape","C:/absolute"))
      assertEquals(path,DriverPackageArchive.Error.UNSAFE_PATH,check(zip("driver.so" to elf(),path to byteArrayOf(1))))
  }
  @Test fun missingLibraryUnsafeMetadataNameAndUnreadableZipAreRejected() {
    val file=zip("other.so" to elf())
    assertEquals(DriverPackageArchive.Error.NO_LIBRARY,check(file))
    assertEquals(DriverPackageArchive.Error.NO_LIBRARY,check(file,"../other.so"))
    assertEquals(DriverPackageArchive.Error.UNREADABLE,check(temporary.newFile().apply{writeText("not ZIP")}))
  }
  @Test fun normalLicenseSubdirectoriesRemainSupported() {
    assertNull(check(zip("driver.so" to elf(),"LICENSES/MIT.txt" to "license".toByteArray())))
  }
  @Test fun allThreeExistingBundledLibrariesPassWithoutChangingTheirBytes() {
    val assets=File("src/main/assets/gpu_drivers").listFiles()!!.filter{it.extension=="zip"}
    assertEquals(3,assets.size)
    for (file in assets)
      assertNull(file.name,check(file,"libvulkan_malisx2.so",0,Int.MAX_VALUE))
  }
}
