package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRepository
import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRewardResult
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsConstellationProgressRepository(
    private val settings: Settings,
) : ConstellationProgressRepository {

    private val totalProgressFlow = MutableStateFlow(settings.getInt(TOTAL_PROGRESS_KEY, 0).coerceAtLeast(0))

    override fun observeTotalProgress(): Flow<Int> = totalProgressFlow

    override suspend fun getTotalProgress(): Int = settings.getInt(TOTAL_PROGRESS_KEY, 0).coerceAtLeast(0)
    override suspend fun refreshProgress(): Int = getTotalProgress()

    override suspend fun getLastRewardDateIso(): String? = settings.getStringOrNull(LAST_REWARD_DATE_KEY)

    override suspend fun saveTotalProgress(value: Int) {
        val safeValue = value.coerceAtLeast(0)
        settings.putInt(TOTAL_PROGRESS_KEY, safeValue)
        totalProgressFlow.value = safeValue
    }

    override suspend fun saveLastRewardDateIso(value: String) {
        settings.putString(LAST_REWARD_DATE_KEY, value)
    }

    override suspend fun claimDailyProgress(todayIso: String, maxTotalProgress: Int): ConstellationProgressRewardResult {
        val lastRewardDate = getLastRewardDateIso()
        val previousTotal = getTotalProgress().coerceIn(0, maxTotalProgress)
        if (lastRewardDate == todayIso) {
            return ConstellationProgressRewardResult(previousTotal, previousTotal, rewarded = false, isComplete = previousTotal >= maxTotalProgress)
        }
        val isComplete = previousTotal >= maxTotalProgress
        val totalProgress = if (isComplete) previousTotal else (previousTotal + 1).coerceAtMost(maxTotalProgress)
        saveTotalProgress(totalProgress)
        saveLastRewardDateIso(todayIso)
        return ConstellationProgressRewardResult(totalProgress, previousTotal, rewarded = !isComplete, isComplete = totalProgress >= maxTotalProgress)
    }
}

private const val TOTAL_PROGRESS_KEY = "constellation_total_progress"
private const val LAST_REWARD_DATE_KEY = "constellation_last_reward_date"
