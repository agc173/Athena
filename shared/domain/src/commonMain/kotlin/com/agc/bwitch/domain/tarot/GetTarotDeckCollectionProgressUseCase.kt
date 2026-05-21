package com.agc.bwitch.domain.tarot

class GetTarotDeckCollectionProgressUseCase(
    private val repository: TarotDeckCollectionRepository,
) {
    suspend operator fun invoke(): Map<String, TarotDeckCollectionProgress> =
        repository.getProgressByTrackId()
}
