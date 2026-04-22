package com.agc.bwitch.domain.economy

interface EconomyRepository {
    suspend fun getBalance(): EconomyBalance
    suspend fun getStatus(): EconomyStatus
}
