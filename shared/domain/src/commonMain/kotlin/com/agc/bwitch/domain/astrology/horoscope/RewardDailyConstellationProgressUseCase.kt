package com.agc.bwitch.domain.astrology.horoscope

class RewardDailyConstellationProgressUseCase(
    private val repository: ConstellationProgressRepository,
) {
    suspend operator fun invoke(todayIso: String, maxTotalProgress: Int): ConstellationProgressRewardResult {
        return repository.claimDailyProgress(todayIso = todayIso, maxTotalProgress = maxTotalProgress)
    }
}

data class ConstellationProgressRewardResult(
    val totalProgress: Int,
    val previousTotalProgress: Int,
    val rewarded: Boolean,
    val isComplete: Boolean,
)
