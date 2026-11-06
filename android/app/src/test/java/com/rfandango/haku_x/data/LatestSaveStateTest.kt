package com.rfandango.haku_x.data

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LatestSaveStateTest {
  @get:Rule val temporary = TemporaryFolder()
  private val baseTime = 1_700_000_000_000L

  private fun state(slot: Int, seconds: Long): File = File(temporary.root, "slot$slot.hxs").apply {
    writeText("state data")
    assertTrue(setLastModified(baseTime + seconds * 1000))
  }

  @Test fun noStatesDoesNotOfferResume() {
    assertNull(SaveStateStore.latestIn(temporary.root))
    assertNull(SaveStateStore.latestIn(File(temporary.root, "missing")))
  }

  @Test fun newestManualStateWinsOverOlderAutomaticAndQuickStates() {
    state(SaveStateStore.AUTO_SLOT, 10)
    state(SaveStateStore.QUICK_SLOT, 20)
    state(7, 30)
    assertEquals(SaveStateStore.SavedState(7, baseTime + 30_000), SaveStateStore.latestIn(temporary.root))
  }

  @Test fun automaticAndQuickStatesCanEachBeTheLatest() {
    state(1, 10)
    state(SaveStateStore.QUICK_SLOT, 20)
    assertEquals(SaveStateStore.QUICK_SLOT, SaveStateStore.latestIn(temporary.root)?.slot)
    state(SaveStateStore.AUTO_SLOT, 30)
    assertEquals(SaveStateStore.AUTO_SLOT, SaveStateStore.latestIn(temporary.root)?.slot)
  }

  @Test fun thumbnailsTemporaryFilesUnknownSlotsAndEmptyStatesAreIgnored() {
    state(10, 10)
    state(12, 100)
    File(temporary.root, "slot1.jpg").writeText("thumbnail")
    File(temporary.root, "slot2.hxs.tmp").writeText("unfinished save")
    File(temporary.root, "slot3.hxs").writeBytes(byteArrayOf())
    File(temporary.root, "slot4.hxs").mkdir()
    assertEquals(10, SaveStateStore.latestIn(temporary.root)?.slot)
  }

  @Test fun equalTimestampsUseStableAutomaticThenQuickThenManualOrder() {
    state(1, 10)
    state(2, 10)
    state(SaveStateStore.QUICK_SLOT, 10)
    state(SaveStateStore.AUTO_SLOT, 10)
    assertEquals(SaveStateStore.AUTO_SLOT, SaveStateStore.latestIn(temporary.root)?.slot)
  }

  @Test fun recheckingAfterRemovalSelectsTheRemainingLatestState() {
    state(4, 10)
    val newest = state(5, 20)
    assertEquals(5, SaveStateStore.latestIn(temporary.root)?.slot)
    assertTrue(newest.delete())
    assertEquals(4, SaveStateStore.latestIn(temporary.root)?.slot)
  }
}
