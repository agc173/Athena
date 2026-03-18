package com.agc.bwitch.domain.astrology.birthchart

class PullBirthEssenceUseCase(
    private val sync: BirthChartSyncController
) {
    suspend operator fun invoke() = sync.pull()
}
