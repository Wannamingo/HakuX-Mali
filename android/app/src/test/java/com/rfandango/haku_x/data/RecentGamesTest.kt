package com.rfandango.haku_x.data

import org.junit.Assert.*
import org.junit.Test

class RecentGamesTest {
  @Test fun relaunchMovesOneGameToFrontAndKeepsOtherOrder() {
    assertEquals(listOf("b", "a", "c"), RecentGames.record(listOf("a", "b", "c"), "b"))
  }
  @Test fun historyIsBoundedAndCorruptDuplicatesDoNotMultiply() {
    assertEquals(listOf("z", "a", "b"), RecentGames.record(listOf("a", "a", "b", "c", ""), "z", 3))
    assertEquals(listOf("a"), RecentGames.record(listOf("a", "a"), ""))
  }
}
