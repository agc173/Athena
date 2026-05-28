package com.agc.bwitch.domain.astrology.horoscope

class RewardDailyConstellationProgressUseCase(
    private val repository: ConstellationProgressRepository,
) {
    suspend operator fun invoke(todayIso: String, maxTotalProgress: Int): ConstellationProgressRewardResult {
        val lastRewardDate = repository.getLastRewardDateIso()
        val previousTotal = repository.getTotalProgress().coerceIn(0, maxTotalProgress)
        if (lastRewardDate == todayIso) {
            return ConstellationProgressRewardResult(
                totalProgress = previousTotal,
                previousTotalProgress = previousTotal,
                rewarded = false,
                isComplete = previousTotal >= maxTotalProgress,
            )
        }
        val isComplete = previousTotal >= maxTotalProgress
        val totalProgress = if (isComplete) previousTotal else (previousTotal + 1).coerceAtMost(maxTotalProgress)
        repository.saveTotalProgress(totalProgress)
        repository.saveLastRewardDateIso(todayIso)
        return ConstellationProgressRewardResult(
            totalProgress = totalProgress,
            previousTotalProgress = previousTotal,
            rewarded = !isComplete,
            isComplete = totalProgress >= maxTotalProgress,
        )
    }
}

data class ConstellationProgressRewardResult(
    val totalProgress: Int,
    val previousTotalProgress: Int,
    val rewarded: Boolean,
    val isComplete: Boolean,
)
