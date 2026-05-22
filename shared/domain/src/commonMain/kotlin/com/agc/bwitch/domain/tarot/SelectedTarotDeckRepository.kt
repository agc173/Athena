package com.agc.bwitch.domain.tarot

interface SelectedTarotDeckRepository {
    fun getSelectedDeckId(): TarotDeckId
    fun setSelectedDeckId(deckId: TarotDeckId)
}

