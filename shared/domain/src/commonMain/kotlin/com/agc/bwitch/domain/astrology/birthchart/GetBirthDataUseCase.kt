package com.agc.bwitch.domain.astrology.birthchart

class GetBirthDataUseCase(
    private val repository: BirthChartRepository
) {
    suspend operator fun invoke(): BirthData? =
        repository.getBirthData()
}

