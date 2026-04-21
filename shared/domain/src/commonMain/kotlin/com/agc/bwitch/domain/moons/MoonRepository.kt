package com.agc.bwitch.domain.moons

import kotlinx.coroutines.flow.Flow

interface MoonRepository {
    suspend fun getBalance(): MoonBalance
    fun observeBalance(): Flow<MoonBalance>
    suspend fun addMoons(amount: Int): MoonBalance
    suspend fun spendMoons(amount: Int): SpendMoonsResult
    suspend fun hasEnough(amount: Int): Boolean
}

interface MoonPackRepository {
    suspend fun getMoonPacks(): List<MoonPack>
}
