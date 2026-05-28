package com.agc.bwitch.domain.astrology.horoscope

class RefreshConstellationProgressUseCase(
    private val repository: ConstellationProgressRepository,
) {
    suspend operator fun invoke(): Int = repository.refreshProgress()
}
