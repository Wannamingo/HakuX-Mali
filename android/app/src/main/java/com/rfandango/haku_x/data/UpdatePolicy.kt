package com.rfandango.haku_x.data

/** This fork's release feed, revision ordering and in-place APK identity. */
object UpdatePolicy {
  const val REPO = "Wannamingo/hakuX-for-RG477M"
  const val LATEST_URL = "https://api.github.com/repos/$REPO/releases/latest"

  fun isApkAsset(name: String, url: String): Boolean =
    name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://github.com/$REPO/releases/download/")

  fun compareVersions(a: String, b: String): Int {
    fun parts(v: String) = v.trim().removePrefix("v").removePrefix("V").substringBefore('+')
      .split('.', '-').map { p -> p.takeWhile(Char::isDigit).toLongOrNull() ?: 0L }
    val x = parts(a)
    val y = parts(b)
    for (i in 0 until maxOf(x.size, y.size)) {
      val c = x.getOrElse(i) { 0L }.compareTo(y.getOrElse(i) { 0L })
      if (c != 0) return c
    }
    return 0
  }

  fun acceptsApk(installedPackage: String, candidatePackage: String, ours: Set<String>, theirs: Set<String>): Boolean =
    installedPackage == candidatePackage && ours.isNotEmpty() && ours == theirs
}
