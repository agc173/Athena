package com.agc.bwitch.domain.astrology.birthchart

class SaveBirthEssenceUseCase(
    private val repository: BirthChartRepository
) {
    suspend operator fun invoke(data: BirthEssenceDraft) =
        repository.saveBirthEssence(data)
}
