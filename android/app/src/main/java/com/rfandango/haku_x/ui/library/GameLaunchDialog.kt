package com.rfandango.haku_x.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rfandango.haku_x.R
import com.rfandango.haku_x.data.DiscInfo
import com.rfandango.haku_x.data.Game
import com.rfandango.haku_x.data.GameOverrides
import com.rfandango.haku_x.data.SaveStateStore
import com.rfandango.haku_x.ui.common.rememberFocusOnShow
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Shared by the full library, search results and recent-game cards. */
@Composable
internal fun GameLaunchDialog(
  game: Game,
  disc: DiscInfo?,
  onLaunch: (Game) -> Unit,
  onLoadState: (Game, SaveStateStore, Int) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val key = GameOverrides.gameKey(disc?.titleId ?: 0L, disc?.version ?: 0L, game.relativePath)
  var store by remember(game.uri, key) { mutableStateOf<SaveStateStore?>(null) }
  var latest by remember(game.uri, key) { mutableStateOf<SaveStateStore.SavedState?>(null) }
  var checking by remember(game.uri, key) { mutableStateOf(true) }
  var failed by remember(game.uri, key) { mutableStateOf(false) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(game.uri, key) {
    val result = withContext(Dispatchers.IO) {
      runCatching {
        val states = SaveStateStore(context, key, game.uri.toString())
        states to states.latest()
      }
    }
    result.onSuccess { (states, saved) -> store = states; latest = saved }
    failed = result.isFailure
    checking = false
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(R.string.library_launch_title, game.title), maxLines = 3, overflow = TextOverflow.Ellipsis)
    },
    text = {
      Column(
        Modifier.fillMaxWidth().heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(stringResource(R.string.library_launch_message))
        when {
          checking -> {
            Text(stringResource(R.string.library_launch_checking))
            LinearProgressIndicator(Modifier.fillMaxWidth())
          }
          failed -> Text(stringResource(R.string.library_launch_check_failed))
          latest == null -> Text(stringResource(R.string.library_launch_no_state))
          else -> latest?.let { saved ->
            val locale = configuration.locales[0]
            val date = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale).format(Date(saved.modifiedAt))
            Text(
              stringResource(R.string.library_launch_last_detail, SaveStateStore.label(context, saved.slot), date),
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
        OutlinedButton(
          onClick = { onDismiss(); onLaunch(game) },
          enabled = !checking,
          modifier = Modifier.fillMaxWidth().then(
            if (!checking && latest == null) Modifier.focusRequester(rememberFocusOnShow()) else Modifier,
          ),
        ) { Text(stringResource(R.string.library_launch_new)) }
        Button(
          enabled = !checking && latest != null,
          modifier = Modifier.fillMaxWidth().then(
            if (!checking && latest != null) Modifier.focusRequester(rememberFocusOnShow()) else Modifier,
          ),
          onClick = {
            val states = store
            if (!checking && states != null) {
              checking = true
              scope.launch {
                // A removed state must never turn a requested resume into a cold start.
                val result = withContext(Dispatchers.IO) { runCatching { states.latest() } }
                val saved = result.getOrNull()
                if (saved != null) {
                  onDismiss()
                  onLoadState(game, states, saved.slot)
                } else {
                  latest = null
                  failed = result.isFailure
                  checking = false
                }
              }
            }
          },
        ) { Text(stringResource(R.string.library_launch_latest)) }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
    },
  )
}
