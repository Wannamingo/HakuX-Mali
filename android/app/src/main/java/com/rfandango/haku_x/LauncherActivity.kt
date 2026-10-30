package com.rfandango.haku_x

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.rfandango.haku_x.data.AppPrefs
import com.rfandango.haku_x.data.Game
import com.rfandango.haku_x.data.GameSources
import com.rfandango.haku_x.data.PreviousVersion
import com.rfandango.haku_x.data.SaveStateStore
import com.rfandango.haku_x.data.Storage
import com.rfandango.haku_x.data.hdd.HddStore
import com.rfandango.haku_x.input.InputCapture
import com.rfandango.haku_x.ui.HakuXApp
import com.rfandango.haku_x.ui.theme.AppTheme
import com.rfandango.haku_x.ui.theme.HakuXTheme
import com.rfandango.haku_x.ui.theme.appInDarkTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.CancellationException
import android.os.SystemClock
import android.os.Handler
import android.os.Looper

/**
 * The app's one screen host (Compose: setup, library, settings), and the
 * entry for frontends that launch a game directly (a VIEW intent with the
 * image, or a rom_path extra): those go straight to the core.
 */
class LauncherActivity : ComponentActivity() {
  override fun attachBaseContext(base: android.content.Context) {
    super.attachBaseContext(com.rfandango.haku_x.data.UiLanguage.wrap(base))
  }
  private data class Launch(val uri: Uri, val path: String?, val title: String?, val external: Boolean, val state: String? = null)
  private var preparing by mutableStateOf<GameSources.Progress?>(null)
  private var choices by mutableStateOf<List<GameSources.Choice>?>(null)
  private var preparationError by mutableStateOf<String?>(null)
  private var pending: Launch? = null
  private var preparationJob: Job? = null

  private fun prepareLaunch(request: Launch, selection: String? = null) {
    if (preparationJob?.isActive == true) return
    pending = request
    choices = null
    preparationError = null
    preparing = GameSources.Progress()
    preparationJob = lifecycleScope.launch {
      try {
        val result = withContext(Dispatchers.IO) {
          val running = currentCoroutineContext()
          val main = Handler(Looper.getMainLooper())
          var last = 0L
          GameSources.prepare(this@LauncherActivity, request.uri, selection, { update ->
            val now = SystemClock.elapsedRealtime()
            if (now - last >= 100) {
              last = now
              main.post { if (running.isActive) preparing = update }
            }
          }, { !running.isActive })
        }
        preparing = null
        when (result) {
          is GameSources.Result.Choose -> choices = result.choices
          is GameSources.Result.Ready -> {
            startCore(request.uri, request.path, request.title, request.external, request.state)
          }
        }
      } catch (_: CancellationException) {
        preparing = null
      } catch (e: Exception) {
        preparing = null
        Log.e(TAG, "Game preparation failed", e)
        if (currentCoroutineContext().isActive) {
          preparationError = e.message ?: e.toString()
        }
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val prefs = AppPrefs(this)

    val usableFolders = prefs.gamesFolders.filter { Storage.hasReadPermission(this, it) }
    val folderReady = usableFolders.isNotEmpty()
    val lastGame = prefs.lastGameUri
    val lastGameUsable = prefs.lastGamePath?.let { File(it).isFile } == true ||
      (lastGame != null && (Storage.hasReadPermission(this, lastGame) || folderReady))
    prefs.dropStale(lastGameUsable, usableFolders)
    Thread { HddStore(this).deleteLeftovers() }.start()
    // the first start after the update from the xemu-based release: its data comes over first
    val needsMigration = !prefs.previousMigrated && PreviousVersion(this).find() != null

    AppTheme.load(this)
    var windowDark = AppTheme.isDark(this)
    windowTheme(windowDark)
    setContent {
      val dark = appInDarkTheme()
      if (dark != windowDark) {
        SideEffect {
          windowDark = dark
          windowTheme(dark)
        }
      }
      HakuXTheme(dark) {
        HakuXApp(
          initialSettingsGameKey = intent.getStringExtra(EXTRA_SETTINGS_GAME_KEY),
          initialSettingsGameTitle = intent.getStringExtra(EXTRA_SETTINGS_GAME_TITLE),
          onClose = { finish() },
          needsMigration = needsMigration,
          needsSetup = !folderReady,
          onLaunchGame = { game: Game ->
            Log.i(TAG, "launch: path=${game.relativePath} uri=${game.uri} size=${game.sizeBytes}")
            Storage.persistPermission(this, game.uri)
            prepareLaunch(Launch(game.uri, game.relativePath, game.title, false))
          },
          onLaunchState = { game: Game, store: SaveStateStore, slot: Int ->
            Log.i(TAG, "launch: path=${game.relativePath} into savestate slot $slot")
            Storage.persistPermission(this, game.uri)
            /* the state's frame on screen while the core starts (NativeCoreActivity holds it) */
            val held = RelaunchActivity.heldFrame(this)
            runCatching { store.thumb(slot).copyTo(held, overwrite = true) }.onFailure { held.delete() }
            prepareLaunch(Launch(game.uri, game.relativePath, game.title, false, store.state(slot).absolutePath))
          },
        )
        preparing?.let {
          AlertDialog(
            onDismissRequest = { preparationJob?.cancel() },
            title = { Text(getString(R.string.game_prepare_title)) },
            text = {
              Column {
                Text(getString(R.string.game_prepare_index))
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
              }
            },
            confirmButton = { TextButton(onClick = { preparationJob?.cancel() }) { Text(getString(android.R.string.cancel)) } },
          )
        }
        choices?.let { list ->
          AlertDialog(
            onDismissRequest = { choices = null },
            title = { Text(getString(R.string.game_prepare_choose)) },
            text = {
              Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                for (choice in list) TextButton(onClick = { pending?.let { prepareLaunch(it, choice.key) } }) { Text(choice.label) }
              }
            },
            confirmButton = { TextButton(onClick = { choices = null }) { Text(getString(android.R.string.cancel)) } },
          )
        }
        preparationError?.let { message ->
          AlertDialog(
            onDismissRequest = { preparationError = null },
            title = { Text(getString(R.string.game_prepare_failed)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { preparationError = null }) { Text(getString(android.R.string.ok)) } },
          )
        }
      }
    }
    if (folderReady) launchExternal()
  }

  /** The system bars' icons and the window behind Compose, light or dark as the app's theme is. */
  private fun windowTheme(dark: Boolean) {
    val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    window.setBackgroundDrawable(ColorDrawable(if (dark) DARK_WINDOW else Color.WHITE))
  }

  /** The core runs in its own process; this one is done until the next launch. */
  private fun startCore(
    uri: Uri,
    relativePath: String?,
    title: String?,
    launchedExternally: Boolean,
    state: String? = null,
  ) {
    AppPrefs(this).rememberLaunch(uri, relativePath, title)
    com.rfandango.haku_x.data.LibraryPresentation(this).record(uri.toString())
    startActivity(Intent(this, NativeCoreActivity::class.java).apply {
      putExtra(NativeCoreActivity.EXTRA_DVD_URI, uri.toString())
      putExtra(EXTRA_LAUNCHED_EXTERNALLY, launchedExternally)
      if (state != null) putExtra(NativeCoreActivity.EXTRA_LOADSTATE, state)
      /* a frontend's: the game alone in the app's task (a library left in it
       * goes), so that what's under it when it closes is the frontend */
      if (launchedExternally) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    })
    finish()
  }

  /* A frontend's launch while the library is open (singleTop). */
  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    if (AppPrefs(this).gamesFolders.any { Storage.hasReadPermission(this, it) }) {
      launchExternal()
    }
  }

  /** The game a frontend asked for, straight to the core; false: none asked. */
  private fun launchExternal(): Boolean {
    val external = externalGame() ?: return false
    Storage.persistPermission(this, external, write = false)
    prepareLaunch(Launch(external, null, null, true))
    return true
  }

  private fun externalGame(): Uri? {
    intent?.data?.let {
      return if (it.scheme == "file" && it.path != null) com.rfandango.haku_x.data.GameDocuments.fromPath(this, it.path!!) else it
    }
    val path = intent?.getStringExtra("rom_path") ?: return null
    return com.rfandango.haku_x.data.GameDocuments.fromPath(this, path)
  }

  /* Controllers' events to the controller settings first (InputCapture). */
  override fun dispatchKeyEvent(event: KeyEvent): Boolean =
    InputCapture.dispatch(event) || super.dispatchKeyEvent(event)

  override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean =
    InputCapture.dispatch(event) || super.dispatchGenericMotionEvent(event)

  companion object {
    const val EXTRA_SETTINGS_GAME_KEY = "settings_game_key"
    const val EXTRA_SETTINGS_GAME_TITLE = "settings_game_title"
    private const val TAG = "hakuX-library"
    /** The dark theme's background (ui/theme Theme.kt), behind Compose. */
    private const val DARK_WINDOW = 0xFF121212.toInt()
    /** Set when a frontend launched the game (no library to go back to). */
    const val EXTRA_LAUNCHED_EXTERNALLY = "launched_externally"
  }
}
