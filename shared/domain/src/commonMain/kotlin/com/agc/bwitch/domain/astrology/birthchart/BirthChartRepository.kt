package com.agc.bwitch.domain.astrology.birthchart

import kotlinx.coroutines.flow.Flow

interface BirthChartRepository {
    fun observeBirthData(): Flow<BirthData?>
    suspend fun getBirthData(): BirthData?
    suspend fun saveBirthData(data: BirthData)
}

