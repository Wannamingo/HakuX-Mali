package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
  @Test fun forkFeedAndAssets() {
    assertEquals("https://api.github.com/repos/Wannamingo/HakuX-Mali/releases/latest", UpdatePolicy.LATEST_URL)
    assertTrue(UpdatePolicy.isApkAsset("HakuX-Mali-1.0-release.apk", "https://github.com/Wannamingo/HakuX-Mali/releases/download/v1.2.0-rg477m.2/HakuX-Mali-1.0-release.apk"))
    assertFalse(UpdatePolicy.isApkAsset("hakuX.apk", "https://github.com/rfandango/hakuX/releases/download/v1.2.0/hakuX.apk"))
    assertFalse(UpdatePolicy.isApkAsset("source.zip", "https://github.com/Wannamingo/HakuX-Mali/releases/download/v1/source.zip"))
  }

  @Test fun forkRevisionOrdering() {
    assertTrue(UpdatePolicy.compareVersions("1.2.0-rg477m.2", "1.2.0-rg477m.1") > 0)
    assertTrue(UpdatePolicy.compareVersions("v1.2.0-rg477m.10", "1.2.0-rg477m.2") > 0)
    assertTrue(UpdatePolicy.compareVersions("1.3.0-rg477m.1", "1.2.0-rg477m.10") > 0)
    assertTrue(UpdatePolicy.compareVersions("1.2.0-rg477m.1", "1.2.0-rg477m.2") < 0)
    assertEquals(0, UpdatePolicy.compareVersions("1.2.0-rg477m.2", "v1.2.0-rg477m.2+build.17"))
  }

  @Test fun numericVersions() {
    assertEquals(0, UpdatePolicy.compareVersions("v1.0", "1.0"))
    assertTrue(UpdatePolicy.compareVersions("1.0.1", "1.0") > 0)
    assertTrue(UpdatePolicy.compareVersions("1.1", "1.0.9") > 0)
    assertTrue(UpdatePolicy.compareVersions("1.10.0-rg477m.1", "1.9.0-rg477m.1") > 0)
    assertEquals(0, UpdatePolicy.compareVersions("1.2", "1.2.0"))
  }

  @Test fun packageAndCertificateMustBothMatch() {
    val pkg = "com.rfandango.haku_x.debug"
    assertTrue(UpdatePolicy.acceptsApk(pkg, pkg, setOf("our certificate"), setOf("our certificate")))
    assertFalse(UpdatePolicy.acceptsApk(pkg, "com.rfandango.haku_x", setOf("our certificate"), setOf("our certificate")))
    assertFalse(UpdatePolicy.acceptsApk(pkg, pkg, setOf("our certificate"), setOf("another certificate")))
    assertFalse(UpdatePolicy.acceptsApk(pkg, pkg, emptySet(), emptySet()))
  }
}
