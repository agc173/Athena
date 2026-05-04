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
    suspend fun authorizeSynastry(requestId: String, languageCode: String?): SynastryAuthorizationResult
    suspend fun authorizePendulum(requestId: String, languageCode: String?): PendulumAuthorizationResult
}

data class PendulumAuthorizationResult(
    val authorized: Boolean,
    val economyDisabled: Boolean = false,
    val status: String? = null,
    val source: String? = null,
    val moonCost: Int = 0,
)


data class SynastryAuthorizationResult(
    val authorized: Boolean,
    val economyDisabled: Boolean = false,
    val status: String? = null,
    val source: String? = null,
    val moonCost: Int = 0,
)
