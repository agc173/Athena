package com.agc.bwitch.domain.tarot

class GetSelectedTarotDeckUseCase(
    private val repository: SelectedTarotDeckRepository,
) {
    operator fun invoke(): TarotDeckId = repository.getSelectedDeckId()
}
