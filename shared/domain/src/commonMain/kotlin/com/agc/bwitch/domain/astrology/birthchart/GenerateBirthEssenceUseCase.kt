package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.shared.ApiResult

class GenerateBirthEssenceUseCase(
    private val repository: BirthChartRepository,
) {
    suspend operator fun invoke(input: BirthEssenceInput): ApiResult<BirthEssenceReading> =
        repository.generateBirthEssence(input)
}
