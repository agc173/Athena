package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryBirthChartRepository : BirthChartRepository {

    private val mutex = Mutex()
    private var cached: BirthData? = null

    override suspend fun getBirthData(): BirthData? =
        mutex.withLock { cached }

    override suspend fun saveBirthData(data: BirthData) {
        mutex.withLock {
            cached = data
        }
    }
}
