package com.agc.bwitch.data.tarot.dto

import com.agc.bwitch.data.remote.economy.DeckCardUnlockRewardDto
import kotlinx.serialization.Serializable

@Serializable
data class TarotDrawRequestDto(
    val requestType: String,
    val requestId: String,
    // App language propagated from presentation to backend callable.
    val lang: String? = null,
    val question: String? = null,
    val adUnlock: AdUnlockDto? = null,
)

@Serializable
data class AdUnlockDto(
    val rewardedProof: String,
)

@Serializable
data class TarotDrawResponseDto(
    val requestId: String,
    val status: String,
    val draw: DrawDto? = null,
    val reading: ReadingDto? = null,
    val error: ErrorDto? = null,
    val requestType: String? = null,
    val quotaSnapshot: QuotaSnapshotDto? = null,
    val systemMode: String? = null,
    val deckId: String? = null,
    val deckCardUnlockRewards: List<DeckCardUnlockRewardDto> = emptyList(),
)

@Serializable
data class QuotaSnapshotDto(
    val freeTarot1Remaining: Int? = null,
    val adUnlockRemaining: Int? = null,
    val maxRequestsRemaining: Int? = null,
    val tarot3Remaining: Int? = null,
)

@Serializable
data class ErrorDto(
    val message: String? = null,
)

@Serializable
data class DrawDto(
    val type: String? = null,
    val card: DrawCardDto? = null,
    val cards: List<DrawCardDto>? = null,
)

@Serializable
data class DrawCardDto(
    val id: String,
    val name: String,
    val orientation: String? = null,
    val position: String? = null,
)

@Serializable
data class ReadingDto(
    val type: String,
    val interpretation: InterpretationDto? = null,
    val cards: List<ReadingCardDto>? = null,
    val summary: String? = null,
    val advice: String? = null,
)

@Serializable
data class InterpretationDto(
    val theme: String? = null,
    val meaning: String? = null,
    val advice: String? = null,
    val watchOut: String? = null,
)

@Serializable
data class ReadingCardDto(
    val position: String? = null,
    val meaning: String? = null,
    val name: String? = null,
    val orientation: String? = null,
)
