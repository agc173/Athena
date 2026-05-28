package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRepository
import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRewardResult
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable

class BackendFirstConstellationProgressRepository(
    private val localRepository: SettingsConstellationProgressRepository,
    private val authRepository: AuthRepository,
    private val functionsClient: FunctionsClient,
) : ConstellationProgressRepository {

    override fun observeTotalProgress(): Flow<Int> = localRepository.observeTotalProgress()
    override suspend fun getTotalProgress(): Int = localRepository.getTotalProgress()
    override suspend fun getLastRewardDateIso(): String? = localRepository.getLastRewardDateIso()
    override suspend fun saveTotalProgress(value: Int) = localRepository.saveTotalProgress(value)
    override suspend fun saveLastRewardDateIso(value: String) = localRepository.saveLastRewardDateIso(value)

    override suspend fun claimDailyProgress(todayIso: String, maxTotalProgress: Int): ConstellationProgressRewardResult {
        val isLoggedIn = authRepository.authState.first() != null
        if (!isLoggedIn) {
            return localRepository.claimDailyProgress(todayIso, maxTotalProgress)
        }

        val backendResult = functionsClient.call(
            name = CLAIM_DAILY_CONSTELLATION_PROGRESS_CALLABLE,
            data = ClaimDailyConstellationProgressRequestDto(todayIso = todayIso),
            requestSerializer = ClaimDailyConstellationProgressRequestDto.serializer(),
            responseSerializer = ClaimDailyConstellationProgressResponseDto.serializer(),
        )

        return when (backendResult) {
            is ApiResult.Ok -> {
                val payload = backendResult.value
                localRepository.saveTotalProgress(payload.totalProgress)
                localRepository.saveLastRewardDateIso(todayIso)
                ConstellationProgressRewardResult(
                    totalProgress = payload.totalProgress,
                    previousTotalProgress = payload.previousTotalProgress,
                    rewarded = payload.rewarded,
                    isComplete = payload.isComplete,
                )
            }
            is ApiResult.Err -> localRepository.claimDailyProgress(todayIso, maxTotalProgress)
        }
    }

    private companion object {
        const val CLAIM_DAILY_CONSTELLATION_PROGRESS_CALLABLE = "claimDailyConstellationProgress"
    }
}

@Serializable
private data class ClaimDailyConstellationProgressRequestDto(
    val todayIso: String,
)

@Serializable
private data class ClaimDailyConstellationProgressResponseDto(
    val totalProgress: Int,
    val previousTotalProgress: Int,
    val rewarded: Boolean,
    val isComplete: Boolean,
)
