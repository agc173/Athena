package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceReading
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock

class InMemoryBirthChartRepository : BirthChartRepository {

    private val state = MutableStateFlow<BirthEssenceProfile?>(null)

    override fun observeBirthEssence(): Flow<BirthEssenceProfile?> = state.asStateFlow()

    override suspend fun getBirthEssence(): BirthEssenceProfile? = state.value

    override suspend fun saveBirthEssence(draft: BirthEssenceDraft) {
        val now = Clock.System.now().toEpochMilliseconds()
        val current = state.value
        state.value = BirthEssenceProfile(
            sunSign = draft.sunSign,
            moonSign = draft.moonSign,
            risingSign = draft.risingSign,
            interpretation = draft.interpretation,
            archetype = draft.archetype,
            savedAtEpochMillis = current?.savedAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )
    }

    override suspend fun generateBirthEssence(input: BirthEssenceInput): ApiResult<BirthEssenceReading> =
        ApiResult.Ok(
            BirthEssenceReading(
                interpretation = "Tu combinación ${input.sunSign.name.lowercase()}-${input.moonSign.name.lowercase()}-${input.risingSign.name.lowercase()} trae una energía versátil y en evolución.",
                archetype = null,
            )
        )
}
