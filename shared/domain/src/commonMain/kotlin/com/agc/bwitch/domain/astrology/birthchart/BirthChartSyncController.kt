package com.agc.bwitch.domain.astrology.birthchart

interface BirthChartSyncController {
    suspend fun pull()
}