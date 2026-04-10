package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class GenerateBirthEssenceUseCaseTest {

    @Test
    fun invoke_propagates_input_language_when_repository_returns_blank_language() = runBlocking {
        val repository = FakeBirthChartRepository(
            generateResult = ApiResult.Ok(
                BirthEssenceReading(
                    interpretation = "Test interpretation",
                    languageCode = "",
                )
            )
        )
        val useCase = GenerateBirthEssenceUseCase(repository)

        val result = useCase(
            BirthEssenceInput(
                sunSign = ZodiacSign.aries,
                moonSign = ZodiacSign.leo,
                risingSign = ZodiacSign.sagittarius,
                languageCode = "en",
            )
        ) as ApiResult.Ok

        assertEquals("en", result.value.languageCode)
    }

    @Test
    fun invoke_sets_resolved_archetype_in_result() = runBlocking {
        val repository = FakeBirthChartRepository(
            generateResult = ApiResult.Ok(
                BirthEssenceReading(
                    interpretation = "Test interpretation",
                    languageCode = "es",
                    archetype = null,
                )
            )
        )
        val useCase = GenerateBirthEssenceUseCase(repository)

        val result = useCase(
            BirthEssenceInput(
                sunSign = ZodiacSign.aries,
                moonSign = ZodiacSign.aries,
                risingSign = ZodiacSign.aries,
                languageCode = "es",
            )
        ) as ApiResult.Ok

        assertEquals(BirthEssenceArchetype.GUERRERA, result.value.archetype)
    }

    private class FakeBirthChartRepository(
        private val generateResult: ApiResult<BirthEssenceReading>,
    ) : BirthChartRepository {
        override fun observeBirthEssence(): Flow<BirthEssenceProfile?> = flowOf(null)

        override suspend fun getBirthEssence(): BirthEssenceProfile? = null

        override suspend fun saveBirthEssence(draft: BirthEssenceDraft) = Unit

        override suspend fun generateBirthEssence(input: BirthEssenceInput): ApiResult<BirthEssenceReading> =
            generateResult
    }
}
