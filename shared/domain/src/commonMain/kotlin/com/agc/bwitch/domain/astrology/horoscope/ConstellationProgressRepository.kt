package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.coroutines.flow.Flow

interface ConstellationProgressRepository {
    fun observeTotalProgress(): Flow<Int>
    suspend fun getTotalProgress(): Int
    suspend fun getLastRewardDateIso(): String?
    suspend fun saveTotalProgress(value: Int)
    suspend fun saveLastRewardDateIso(value: String)
}
