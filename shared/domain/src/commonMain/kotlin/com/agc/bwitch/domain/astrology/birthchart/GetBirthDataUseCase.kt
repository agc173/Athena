package com.agc.bwitch.domain.astrology.birthchart

class GetBirthEssenceUseCase(
    private val repository: BirthChartRepository
) {
    suspend operator fun invoke(): BirthEssenceProfile? =
        repository.getBirthEssence()
}
