package com.agc.bwitch.domain.astrology.horoscope

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest

class RewardDailyConstellationProgressUseCaseTest {

    @Test
    fun firstAccessOfDay_rewardsAndIncrements() = runTest {
        val repository = FakeConstellationProgressRepository(totalProgress = 3, lastRewardDateIso = null)
        val useCase = RewardDailyConstellationProgressUseCase(repository)

        val result = useCase(todayIso = "2026-05-28", maxTotalProgress = 10)

        assertTrue(result.rewarded)
        assertEquals(3, result.previousTotalProgress)
        assertEquals(4, result.totalProgress)
        assertFalse(result.isComplete)
        assertEquals(4, repository.getTotalProgress())
        assertEquals("2026-05-28", repository.getLastRewardDateIso())
    }

    @Test
    fun secondAccessSameDay_doesNotRewardAgain() = runTest {
        val repository = FakeConstellationProgressRepository(totalProgress = 4, lastRewardDateIso = "2026-05-28")
        val useCase = RewardDailyConstellationProgressUseCase(repository)

        val result = useCase(todayIso = "2026-05-28", maxTotalProgress = 10)

        assertFalse(result.rewarded)
        assertEquals(4, result.previousTotalProgress)
        assertEquals(4, result.totalProgress)
        assertFalse(result.isComplete)
    }

    @Test
    fun completeProgress_doesNotIncrementAndNotRewarded() = runTest {
        val repository = FakeConstellationProgressRepository(totalProgress = 10, lastRewardDateIso = null)
        val useCase = RewardDailyConstellationProgressUseCase(repository)

        val result = useCase(todayIso = "2026-05-28", maxTotalProgress = 10)

        assertFalse(result.rewarded)
        assertEquals(10, result.previousTotalProgress)
        assertEquals(10, result.totalProgress)
        assertTrue(result.isComplete)
    }

    @Test
    fun completeProgress_stillSavesLastRewardDate() = runTest {
        val repository = FakeConstellationProgressRepository(totalProgress = 10, lastRewardDateIso = null)
        val useCase = RewardDailyConstellationProgressUseCase(repository)

        useCase(todayIso = "2026-05-28", maxTotalProgress = 10)

        assertEquals("2026-05-28", repository.getLastRewardDateIso())
    }

    private class FakeConstellationProgressRepository(
        totalProgress: Int,
        private var lastRewardDateIso: String?,
    ) : ConstellationProgressRepository {
        private val totalProgressFlow = MutableStateFlow(totalProgress)

        override fun observeTotalProgress(): Flow<Int> = totalProgressFlow.asStateFlow()

        override suspend fun getTotalProgress(): Int = totalProgressFlow.value
        override suspend fun refreshProgress(): Int = totalProgressFlow.value

        override suspend fun getLastRewardDateIso(): String? = lastRewardDateIso

        override suspend fun saveTotalProgress(value: Int) {
            totalProgressFlow.value = value
        }

        override suspend fun saveLastRewardDateIso(value: String) {
            lastRewardDateIso = value
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
}
