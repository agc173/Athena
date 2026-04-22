package com.agc.bwitch.data.economy

import com.agc.bwitch.data.remote.economy.EconomyBalanceDto
import com.agc.bwitch.data.remote.economy.EconomyClaimResultDto
import com.agc.bwitch.data.remote.economy.EconomyRemoteDataSource
import com.agc.bwitch.data.remote.economy.EconomyStatusDto
import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
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

    override suspend fun claimDailyLogin(requestId: String): EconomyClaimResult {
        return remoteDataSource
            .claimDailyLogin(requestId = requestId)
            .toDomain()
    }

    override suspend fun claimRewardedAd(
        requestId: String,
        adProof: String,
        placement: String?,
    ): EconomyClaimResult {
        return remoteDataSource
            .claimRewardedAd(
                requestId = requestId,
                adProof = adProof,
                placement = placement,
            )
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

private fun EconomyClaimResultDto.toDomain(): EconomyClaimResult {
    return EconomyClaimResult(
        result = result.toClaimStatus(),
        balance = balance,
        dailyLoginClaimed = dailyLoginClaimed,
        rewardedAdsClaimed = rewardedAdsClaimed,
        rewardedAdsRemaining = rewardedAdsRemaining,
    )
}

private fun String.toClaimStatus(): EconomyClaimStatus {
    return when (this) {
        "CLAIMED" -> EconomyClaimStatus.CLAIMED
        "ALREADY_CLAIMED" -> EconomyClaimStatus.ALREADY_CLAIMED
        "DAILY_LIMIT_REACHED" -> EconomyClaimStatus.DAILY_LIMIT_REACHED
        else -> throw IllegalStateException("Unknown economy claim result: $this")
    }
}
