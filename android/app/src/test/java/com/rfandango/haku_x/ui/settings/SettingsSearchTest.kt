package com.rfandango.haku_x.ui.settings

import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class SettingsSearchTest {
  @Test fun translatedLabelsAreFoundByEitherLanguage() {
    assertTrue(SettingsSearch.matches("fps", "Ограничение кадров", "frame rate fps 120"))
    assertTrue(SettingsSearch.matches("разрешение", "Resolution scale", "масштаб разрешение"))
    assertTrue(SettingsSearch.matches("mali драйвер", "GPU driver", "MaliSX2 драйвер"))
    assertFalse(SettingsSearch.matches("mali температура", "GPU driver", "MaliSX2 драйвер"))
  }
  @Test fun punctuationAndEveryQueryWordMatter() {
    assertTrue(SettingsSearch.matches("16 / 9", "Aspect ratio", "4:3 16:9"))
    assertTrue(SettingsSearch.matches("  120 FPS ", "Frame rate limit", "fps 30 60 120"))
    assertFalse(SettingsSearch.matches(" ", "Frame rate limit"))
    assertFalse(SettingsSearch.matches("?!", "Aspect ratio"))
  }
  @Test fun resultsDoNotDependOnDeviceCaseRules() {
    val before = Locale.getDefault()
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"))
      assertTrue(SettingsSearch.matches("MALI", "MaliSX2"))
      assertTrue(SettingsSearch.matches("темная", "Тёмная тема"))
    } finally { Locale.setDefault(before) }
  }
}
