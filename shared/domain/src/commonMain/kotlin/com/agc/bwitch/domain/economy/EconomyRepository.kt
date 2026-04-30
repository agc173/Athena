package com.agc.bwitch.domain.economy

interface EconomyRepository {
    suspend fun getBalance(): EconomyBalance
    suspend fun getStatus(): EconomyStatus
    suspend fun claimDailyLogin(requestId: String): EconomyClaimResult
    suspend fun claimRewardedAd(
        requestId: String,
        adProof: String,
        placement: String?,
    ): EconomyClaimResult
    suspend fun getModulePreviews(modules: List<String>): List<EconomyModulePreview>
}
