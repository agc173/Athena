package com.agc.bwitch.domain.tarot

data class TarotDeckCollectionProgress(
    val trackId: String,
    val unlockedCards: Set<String>,
)

interface TarotDeckCollectionRepository {
    suspend fun getProgressByTrackId(): Map<String, TarotDeckCollectionProgress>
}
