package com.rfandango.haku_x.overlay

import org.junit.Assert.*
import org.junit.Test

class OverlayEditSessionTest {
  private fun layout() = OverlayLayout(linkedMapOf(
    ControlId.A to ControlSpec(ControlId.A, 900f, 300f, 40f, true, .5f),
    ControlId.MENU to ControlSpec(ControlId.MENU, 500f, 400f, 30f, true, .8f)))

  @Test fun cancelRestoresPositionSizeOpacityAndVisibilityAfterReset() {
    val original = layout(); val session = OverlayEditSession(); session.begin(original)
    original[ControlId.A].apply { cx = 1f; cy = 2f; radius = 64f; opacity = .2f; visible = false }
    val restored = session.cancel()!!
    restored[ControlId.A].let {
      assertEquals(900f, it.cx); assertEquals(300f, it.cy); assertEquals(40f, it.radius)
      assertEquals(.5f, it.opacity); assertTrue(it.visible)
    }
    assertEquals(.8f, restored[ControlId.MENU].opacity)
    assertFalse(session.active)
  }

  @Test fun repeatedBeginDoesNotOverwriteOriginalSnapshot() {
    val draft = layout(); val session = OverlayEditSession(); session.begin(draft)
    draft[ControlId.A].radius = 99f; session.begin(draft)
    assertEquals(40f, session.cancel()!![ControlId.A].radius)
  }

  @Test fun cancelAfterRotationKeepsOriginalNormalizedLayout() {
    val session = OverlayEditSession(); session.begin(layout()); session.resized(.75f, 2f)
    val a = session.cancel()!![ControlId.A]
    assertEquals(675f, a.cx); assertEquals(600f, a.cy); assertEquals(80f, a.radius)
  }

  @Test fun failedSaveRetainsDraftSessionAndOriginalForCancel() {
    val draft = layout(); val session = OverlayEditSession(); session.begin(draft)
    draft[ControlId.A].opacity = .1f
    assertFalse(session.save { false }); assertTrue(session.active)
    assertEquals(.5f, session.cancel()!![ControlId.A].opacity)
  }

  @Test fun successfulSaveIsOneCommitAndCannotBeUndoneByClosingView() {
    val session = OverlayEditSession(); session.begin(layout()); var writes = 0
    assertTrue(session.save { writes++; true }); assertEquals(1, writes)
    assertFalse(session.active); assertNull(session.cancel())
    assertFalse(session.save { writes++; true }); assertEquals(1, writes)
  }
}
