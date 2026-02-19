package com.agc.bwitch.domain.astrology.birthchart

import kotlinx.coroutines.flow.Flow

class ObserveBirthDataUseCase(
    private val repository: BirthChartRepository
) {
    operator fun invoke(): Flow<BirthData?> =
        repository.observeBirthData()
}
