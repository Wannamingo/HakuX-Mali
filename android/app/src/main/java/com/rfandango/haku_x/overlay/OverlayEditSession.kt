package com.rfandango.haku_x.overlay

/** An edit is a draft until persistence succeeds; cancel also undoes resets and rotation. */
class OverlayEditSession {
  private var before: OverlayLayout? = null
  val active get() = before != null

  fun begin(layout: OverlayLayout) {
    if (before == null) before = layout.copy()
  }

  fun resized(sx: Float, sy: Float) {
    require(sx.isFinite() && sy.isFinite() && sx > 0 && sy > 0)
    before?.controls?.values?.forEach { it.cx *= sx; it.cy *= sy; it.radius *= sy }
  }

  fun save(persist: () -> Boolean): Boolean {
    if (!active || !persist()) return false
    before = null
    return true
  }

  fun cancel(): OverlayLayout? = before?.copy().also { before = null }
}
