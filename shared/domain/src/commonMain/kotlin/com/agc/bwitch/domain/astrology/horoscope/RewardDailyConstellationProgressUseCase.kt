package com.agc.bwitch.domain.astrology.horoscope

class RewardDailyConstellationProgressUseCase(
    private val repository: ConstellationProgressRepository,
) {
    suspend operator fun invoke(todayIso: String, maxTotalProgress: Int): Int {
        val lastRewardDate = repository.getLastRewardDateIso()
        val current = repository.getTotalProgress().coerceAtLeast(0)
        if (lastRewardDate == todayIso) return current.coerceAtMost(maxTotalProgress)
        val boundedCurrent = current.coerceAtMost(maxTotalProgress)
        val next = if (boundedCurrent >= maxTotalProgress) {
            boundedCurrent
        } else {
            boundedCurrent + 1
        }
        repository.saveTotalProgress(next.coerceAtMost(maxTotalProgress))
        repository.saveLastRewardDateIso(todayIso)
        return next.coerceAtMost(maxTotalProgress)
    }
}
