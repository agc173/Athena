package com.agc.bwitch.domain.astrology.birthchart

import kotlinx.coroutines.flow.Flow

class ObserveBirthEssenceUseCase(
    private val repository: BirthChartRepository
) {
    operator fun invoke(): Flow<BirthEssenceProfile?> =
        repository.observeBirthEssence()
}
