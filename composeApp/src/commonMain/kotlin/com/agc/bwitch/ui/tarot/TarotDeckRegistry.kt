package com.agc.bwitch.ui.tarot

import com.agc.bwitch.domain.tarot.TarotDeckCatalog
import com.agc.bwitch.domain.tarot.TarotDeckDefinition
import com.agc.bwitch.domain.tarot.TarotDeckId

object TarotDeckRegistry {
    val allDecks: List<TarotDeckDefinition> = TarotDeckCatalog.allDecks

    val defaultDeck: TarotDeckDefinition = TarotDeckCatalog.defaultDeck

    fun getById(id: TarotDeckId): TarotDeckDefinition? =
        TarotDeckCatalog.getById(id)
}

fun TarotDeckDefinition.displayNameLocalized(): String =
    when (id) {
        TarotDeckId.RIDER_WAITE -> "Rider-Waite"
        TarotDeckId.ARCANA_NOCTIS -> "Arcana Noctis"
    }
