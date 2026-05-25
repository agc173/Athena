package com.agc.bwitch.data.economy

import com.agc.bwitch.data.remote.economy.EconomyBalanceDto
import com.agc.bwitch.data.remote.economy.EconomyClaimResultDto
import com.agc.bwitch.data.remote.economy.EconomyRemoteDataSource
import com.agc.bwitch.data.remote.economy.EconomyStatusDto
import com.agc.bwitch.data.remote.economy.EconomyModulePreviewDto
import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus
import com.agc.bwitch.domain.economy.EconomyNextSource
import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.SynastryAuthorizationResult
import com.agc.bwitch.domain.economy.PendulumAuthorizationResult
import com.agc.bwitch.domain.model.DeckCardUnlockReward

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

    override suspend fun getModulePreviews(modules: List<String>): List<EconomyModulePreview> {
        return remoteDataSource
            .getModulePreviews(modules)
            .map { it.toEconomyModulePreview() }
    }

    override suspend fun authorizeSynastry(
        requestId: String,
        languageCode: String?,
    ): SynastryAuthorizationResult {
        return remoteDataSource.authorizeSynastry(requestId, languageCode).toDomain()
    }

    override suspend fun authorizePendulum(requestId: String, languageCode: String?): PendulumAuthorizationResult {
        return remoteDataSource.authorizePendulum(requestId, languageCode).toDomain()
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


private fun EconomyModulePreviewDto.toEconomyModulePreview(): EconomyModulePreview {
    return EconomyModulePreview(
        module = module,
        nextSource = nextSource.toEconomyNextSource(),
        cost = cost,
        balance = balance,
        canExecute = canExecute,
        reasonIfRejected = reasonIfRejected,
        labelKey = labelKey,
        uiHint = uiHint,
        freeRemaining = freeRemaining,
        premiumRemaining = premiumRemaining,
        moonRemaining = moonRemaining,
        moonPackUsesPerMoon = moonPackUsesPerMoon,
        dailyCap = dailyCap,
    )
}

private fun String.toEconomyNextSource(): EconomyNextSource {
    return when (this) {
        "FREE" -> EconomyNextSource.FREE
        "PREMIUM" -> EconomyNextSource.PREMIUM
        "MOON" -> EconomyNextSource.MOON
        "REJECTED" -> EconomyNextSource.REJECTED
        "NOT_CONFIGURED" -> EconomyNextSource.NOT_CONFIGURED
        "COMING_SOON" -> EconomyNextSource.COMING_SOON
        "RULE_CONFIGURED_NOT_WIRED" -> EconomyNextSource.RULE_CONFIGURED_NOT_WIRED
        else -> EconomyNextSource.UNKNOWN
    }
}

private fun com.agc.bwitch.data.remote.economy.SynastryAuthorizeResponseDto.toDomain(): SynastryAuthorizationResult {
    return SynastryAuthorizationResult(
        authorized = authorized,
        economyDisabled = economyDisabled,
        status = status,
        source = source,
        moonCost = moonCost,
        deckCardUnlockRewards = deckCardUnlockRewards.map { it.toDomain() },
    )
}


private fun com.agc.bwitch.data.remote.economy.PendulumAuthorizeResponseDto.toDomain(): PendulumAuthorizationResult {
    return PendulumAuthorizationResult(
        authorized = authorized,
        economyDisabled = economyDisabled,
        status = status,
        source = source,
        moonCost = moonCost,
        deckCardUnlockRewards = deckCardUnlockRewards.map { it.toDomain() },
    )
}

private fun com.agc.bwitch.data.remote.economy.DeckCardUnlockRewardDto.toDomain(): DeckCardUnlockReward =
    DeckCardUnlockReward(deckId = deckId, trackId = trackId, rewardPoolId = rewardPoolId, cardId = cardId)
