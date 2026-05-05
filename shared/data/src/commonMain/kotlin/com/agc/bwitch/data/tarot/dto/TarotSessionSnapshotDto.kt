package com.agc.bwitch.data.tarot.dto

import kotlinx.serialization.Serializable

@Serializable
data class TarotSessionSnapshotDto(
    val requestId: String,
    val type: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val phase: String,
    val responseJson: String? = null,
    val revealPhase: String,
    val revealedCardCount: Int = 0,
    val activeCardIndex: Int = 0,
    val activeCardRevealed: Boolean = false,
    val overlayVisible: Boolean = false,
    val overlayCardIndex: Int? = null,
    val overlayCardRevealed: Boolean = false,
    val openedMiniCardIndex: Int? = null,
)
