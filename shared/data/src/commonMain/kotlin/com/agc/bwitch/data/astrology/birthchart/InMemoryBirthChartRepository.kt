package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemoryBirthChartRepository : BirthChartRepository {

    private val state = MutableStateFlow<BirthData?>(null)

    override fun observeBirthData(): Flow<BirthData?> = state.asStateFlow()

    override suspend fun getBirthData(): BirthData? = state.value

    override suspend fun saveBirthData(data: BirthData) {
        state.value = data
    }
}

