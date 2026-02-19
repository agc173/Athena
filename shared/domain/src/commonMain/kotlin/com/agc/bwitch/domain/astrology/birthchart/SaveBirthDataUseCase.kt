package com.agc.bwitch.domain.astrology.birthchart

class SaveBirthDataUseCase(
    private val repository: BirthChartRepository
) {
    suspend operator fun invoke(data: BirthData) =
        repository.saveBirthData(data)
}
