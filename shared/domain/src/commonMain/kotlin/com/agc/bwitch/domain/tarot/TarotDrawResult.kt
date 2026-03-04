package com.agc.bwitch.domain.tarot

data class TarotCard(
    val id: String,
    val name: String,
    val upright: Boolean? = null,
)

data class TarotReading(
    val text: String,
)

data class TarotDrawResponse(
    val requestId: String,
    val status: String,
    val cards: List<TarotCard>,
    val interpretation: String,
)
