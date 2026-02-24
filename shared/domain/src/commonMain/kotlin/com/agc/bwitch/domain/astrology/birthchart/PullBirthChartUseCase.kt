package com.agc.bwitch.domain.astrology.birthchart

class PullBirthChartUseCase(
    private val sync: BirthChartSyncController
) {
    suspend operator fun invoke() = sync.pull()
}