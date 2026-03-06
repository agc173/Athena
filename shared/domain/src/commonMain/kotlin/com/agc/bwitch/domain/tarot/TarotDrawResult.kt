package com.agc.bwitch.domain.tarot

data class TarotCard(
    val id: String,
    val name: String,
    val upright: Boolean? = null,
    val position: TarotCardPosition? = null,
)

enum class TarotCardPosition {
    PAST,
    PRESENT,
    FUTURE,
}

sealed class TarotReadingDetails {
    data class Tarot1ReadingDetails(
        val theme: String,
        val meaning: String,
        val advice: String,
        val watchOut: String,
    ) : TarotReadingDetails()

    data class Tarot3ReadingDetails(
        val cards: List<Tarot3CardMeaning>,
        val summary: String,
        val advice: String,
    ) : TarotReadingDetails()
}

data class Tarot3CardMeaning(
    val position: TarotCardPosition,
    val meaning: String,
)

data class TarotDrawResponse(
    val requestId: String,
    val status: String,
    val cards: List<TarotCard>,
    val details: TarotReadingDetails? = null,
    val interpretation: String,
)
