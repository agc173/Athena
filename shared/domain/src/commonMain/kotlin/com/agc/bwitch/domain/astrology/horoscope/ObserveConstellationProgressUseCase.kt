package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.coroutines.flow.Flow

class ObserveConstellationProgressUseCase(
    private val repository: ConstellationProgressRepository,
) {
    operator fun invoke(): Flow<Int> = repository.observeTotalProgress()
}
