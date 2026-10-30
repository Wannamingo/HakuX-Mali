package com.rfandango.haku_x.data

object RecentGames {
  fun record(previous: List<String>, uri: String, limit: Int = 12): List<String> {
    require(limit > 0)
    if (uri.isBlank()) return previous.distinct().take(limit)
    return (listOf(uri) + previous.filter { it != uri && it.isNotBlank() }).distinct().take(limit)
  }
}
