package com.rfandango.haku_x.ui.controllers

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.rfandango.haku_x.data.PadOut

@Composable
internal fun StickPreview(pad: PadOut) {
  val traces = remember { Array(2) { mutableStateListOf<Offset>() } }
  val points = listOf(Offset(pad.lx / 32767f, pad.ly / 32767f), Offset(pad.rx / 32767f, pad.ry / 32767f))
  LaunchedEffect(points) {
    points.forEachIndexed { i, p ->
      if (traces[i].size >= 720) traces[i].removeAt(0)
      traces[i].add(p)
    }
  }
  Canvas(Modifier.fillMaxWidth().height(160.dp)) {
    val r = minOf(size.width / 5f, size.height / 2.4f)
    for (i in 0..1) {
      val c = Offset(size.width * (if (i == 0) .25f else .75f), size.height / 2f)
      drawRect(Color.Green, c - Offset(r, r), androidx.compose.ui.geometry.Size(2*r, 2*r), style = Stroke(2f))
      drawCircle(Color.Gray, r, c, style = Stroke(1f))
      for (p in traces[i]) drawCircle(Color.White.copy(alpha = .4f), 2f, c + Offset(p.x*r, -p.y*r))
      drawCircle(Color.Yellow, 6f, c + Offset(points[i].x*r, -points[i].y*r))
    }
  }
}
