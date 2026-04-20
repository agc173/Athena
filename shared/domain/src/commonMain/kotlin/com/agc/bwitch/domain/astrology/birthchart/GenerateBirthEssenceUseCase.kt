package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.shared.ApiResult

class GenerateBirthEssenceUseCase(
    private val repository: BirthChartRepository,
) {
    private val archetypeResolver = BirthEssenceArchetypeResolver()

    suspend operator fun invoke(input: BirthEssenceInput): ApiResult<BirthEssenceReading> {
        val resolvedArchetype = archetypeResolver.resolve(
            sunSign = input.sunSign,
            moonSign = input.moonSign,
            risingSign = input.risingSign,
        )

        return when (val result = repository.generateBirthEssence(input.copy(archetypeHint = resolvedArchetype))) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> ApiResult.Ok(
                result.value.copy(
                    languageCode = result.value.languageCode.ifBlank { input.languageCode },
                    archetype = resolvedArchetype,
                )
            )
        }
    }
}
