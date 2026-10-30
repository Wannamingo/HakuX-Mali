package com.rfandango.haku_x.ui.library

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rfandango.haku_x.data.DiscInfo
import com.rfandango.haku_x.data.Game
import com.rfandango.haku_x.data.LibraryPresentation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun GameCover(game: Game, disc: DiscInfo?, icon: ImageBitmap?, preferences: LibraryPresentation,
  revision: Int, modifier: Modifier = Modifier) {
  val file = remember(game.uri, disc) { preferences.coverFile(game, disc) }
  val cover by produceState<ImageBitmap?>(null, file, revision) {
    value = withContext(Dispatchers.IO) { runCatching { BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull() }
  }
  Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
    if (cover != null) Image(cover!!, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    else if (icon != null) Image(icon, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(16.dp))
    else Text(game.title, style = MaterialTheme.typography.titleMedium, maxLines = 4, overflow = TextOverflow.Ellipsis,
      modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
