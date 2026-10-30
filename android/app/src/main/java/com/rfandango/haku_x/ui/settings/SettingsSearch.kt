package com.rfandango.haku_x.ui.settings

import java.text.Normalizer
import java.util.Locale

/** Match all query words across a translated label and its English/Russian aliases. */
object SettingsSearch {
  fun words(value: String): List<String> = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    .split(Regex("[^\\p{L}\\p{N}]+" )).filter { it.isNotEmpty() }

  fun matches(query: String, label: String, aliases: String = ""): Boolean {
    val tokens = words(query)
    val haystack = words("$label $aliases")
    return tokens.isNotEmpty() && tokens.all { token -> haystack.any { it.contains(token) } }
  }
}
