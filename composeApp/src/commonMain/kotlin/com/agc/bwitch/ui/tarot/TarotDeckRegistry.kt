package com.agc.bwitch.ui.tarot

import com.agc.bwitch.domain.tarot.TarotDeckDefinition
import com.agc.bwitch.domain.tarot.TarotDeckId

object TarotDeckRegistry {
    val allDecks: List<TarotDeckDefinition> = listOf(
        TarotDeckDefinition(
            id = TarotDeckId.RIDER_WAITE,
            displayNameKey = "tarot_deck_rider_waite",
            previewCardId = "major-00-fool",
            isDefault = true,
        ),
        TarotDeckDefinition(
            id = TarotDeckId.ARCANA_NOCTIS,
            displayNameKey = "tarot_deck_arcana_noctis",
            previewCardId = "major-00-fool",
            isDefault = false,
        ),
    )

    val defaultDeck: TarotDeckDefinition = allDecks.first { it.isDefault }

    fun getById(id: TarotDeckId): TarotDeckDefinition? =
        allDecks.firstOrNull { it.id == id }
}
