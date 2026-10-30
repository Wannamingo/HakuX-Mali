package com.rfandango.haku_x.data

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import java.io.File
import android.util.AtomicFile

/** The app's language is independent from the emulated Xbox language. */
object UiLanguage {
  const val SYSTEM = "system"
  val choices = listOf(SYSTEM, "en", "ru")
  private fun file(context: Context) = File(context.filesDir, "ui-language")

  fun current(context: Context): String {
    if (Build.VERSION.SDK_INT >= 33) {
      val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
      return if (locales.isEmpty) SYSTEM else locales[0].language.takeIf { it in choices } ?: SYSTEM
    }
    // Read fresh: the library and the running core live in separate processes.
    return runCatching { file(context).readText().trim().takeIf { it in choices } }.getOrNull() ?: SYSTEM
  }

  fun wrap(base: Context): Context {
    if (Build.VERSION.SDK_INT >= 33) return base
    val language = current(base)
    if (language == SYSTEM) return base
    val config = Configuration(base.resources.configuration)
    val locale = Locale.forLanguageTag(language)
    config.setLocales(LocaleList(locale))
    config.setLayoutDirection(locale)
    return base.createConfigurationContext(config)
  }

  fun set(context: Context, language: String) {
    require(language in choices)
    if (language == current(context)) return
    if (Build.VERSION.SDK_INT >= 33) {
      context.getSystemService(LocaleManager::class.java).applicationLocales =
        if (language == SYSTEM) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(language)
    } else {
      // Commit before activity recreation or the native process reads the preference.
      val atomic = AtomicFile(file(context))
      val output = atomic.startWrite()
      try { output.write(language.toByteArray()); atomic.finishWrite(output) }
      catch (e: Exception) { atomic.failWrite(output); throw e }
      var activity: Context = context
      while (activity is ContextWrapper && activity !is Activity) activity = activity.baseContext
      (activity as? Activity)?.recreate()
    }
  }

  @Suppress("DEPRECATION")
  fun refresh(context: Context) {
    if (Build.VERSION.SDK_INT >= 33) return
    val language = current(context)
    val locale = if (language == SYSTEM) android.content.res.Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(language)
    if (context.resources.configuration.locales[0] == locale) return
    val configuration = Configuration(context.resources.configuration)
    configuration.setLocales(LocaleList(locale)); configuration.setLayoutDirection(locale)
    context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
  }
}
