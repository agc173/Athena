package com.agc.bwitch.presentation.ads

interface RewardedAdsService {
    fun preloadRewardedAd(placement: String) = Unit
    suspend fun showRewardedAd(placement: String): RewardedAdResult
}

sealed interface RewardedAdResult {
    data object Completed : RewardedAdResult
    data class Failed(val reason: String) : RewardedAdResult
    data object Unavailable : RewardedAdResult
    data object Cancelled : RewardedAdResult
}

class NoOpRewardedAdsService : RewardedAdsService {
    override suspend fun showRewardedAd(placement: String): RewardedAdResult = RewardedAdResult.Unavailable
}

