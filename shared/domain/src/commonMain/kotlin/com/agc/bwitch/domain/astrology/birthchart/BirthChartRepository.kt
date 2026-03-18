package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.coroutines.flow.Flow

interface BirthChartRepository {
    fun observeBirthEssence(): Flow<BirthEssenceProfile?>
    suspend fun getBirthEssence(): BirthEssenceProfile?
    suspend fun saveBirthEssence(draft: BirthEssenceDraft)
    suspend fun generateBirthEssence(input: BirthEssenceInput): ApiResult<BirthEssenceReading>
}
