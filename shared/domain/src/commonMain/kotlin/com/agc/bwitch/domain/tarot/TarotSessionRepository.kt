package com.agc.bwitch.domain.tarot

interface TarotSessionRepository {
    suspend fun loadSession(): TarotSessionSnapshot?
    suspend fun saveSession(snapshot: TarotSessionSnapshot)
    suspend fun clearSession()
}

enum class TarotSessionPhase {
    PENDING_BACKEND_REQUEST,
    RESULT_READY_WAITING_SHUFFLE_REVEAL,
    PARTIALLY_REVEALED,
    COMPLETED_READING_VISIBLE,
}

data class TarotSessionSnapshot(
    val requestId: String,
    val type: TarotRequestType,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val phase: TarotSessionPhase,
    val response: TarotDrawResponse? = null,
    val revealPhase: String,
    val revealedCardCount: Int = 0,
    val activeCardIndex: Int = 0,
    val activeCardRevealed: Boolean = false,
    val overlayVisible: Boolean = false,
    val overlayCardIndex: Int? = null,
    val overlayCardRevealed: Boolean = false,
    val openedMiniCardIndex: Int? = null,
)
