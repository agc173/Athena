package com.agc.bwitch.domain.astrology.birthchart

interface BirthChartRepository {
    suspend fun getBirthData(): BirthData?
    suspend fun saveBirthData(data: BirthData)
}
