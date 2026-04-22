package com.agc.bwitch.data.economy

import com.agc.bwitch.data.remote.economy.EconomyBalanceDto
import com.agc.bwitch.data.remote.economy.EconomyRemoteDataSource
import com.agc.bwitch.data.remote.economy.EconomyStatusDto
import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus

class EconomyRepositoryImpl(
    private val remoteDataSource: EconomyRemoteDataSource,
) : EconomyRepository {

    override suspend fun getBalance(): EconomyBalance {
        return remoteDataSource
            .getBalance()
            .toDomain()
    }

    override suspend fun getStatus(): EconomyStatus {
        return remoteDataSource
            .getStatus()
            .toDomain()
    }
}

private fun EconomyBalanceDto.toDomain(): EconomyBalance {
    return EconomyBalance(
        balance = balance,
        dailyLoginClaimed = dailyLoginClaimed,
        rewardedAdsClaimed = rewardedAdsClaimed,
        rewardedAdsRemaining = rewardedAdsRemaining,
    )
}

private fun EconomyStatusDto.toDomain(): EconomyStatus {
    return EconomyStatus(
        balance = balance,
        isPremium = premium.isPremium,
        todayDateIso = todayDateIso,
    )
}
