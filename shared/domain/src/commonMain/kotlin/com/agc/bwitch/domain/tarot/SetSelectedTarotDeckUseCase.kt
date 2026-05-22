package com.agc.bwitch.domain.tarot

class SetSelectedTarotDeckUseCase(
    private val repository: SelectedTarotDeckRepository,
) {
    operator fun invoke(deckId: TarotDeckId) = repository.setSelectedDeckId(deckId)
}
