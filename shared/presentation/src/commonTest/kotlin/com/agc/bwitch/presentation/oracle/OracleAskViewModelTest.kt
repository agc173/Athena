package com.agc.bwitch.presentation.oracle

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.oracle.OracleAskResult
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.presentation.analytics.FakeAnalyticsTracker
import com.agc.bwitch.presentation.oracle.OracleAskMessageId
import kotlin.collections.ArrayDeque
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers

@OptIn(ExperimentalCoroutinesApi::class)
class OracleAskViewModelTest {

    @Test
    fun `ask uses current app language when lang is not provided`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeOracleRepository()
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.English))
            val viewModel = OracleAskViewModel(
                oracleRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                economyRepository = FakeEconomyRepository(),
            )

            viewModel.onQuestionChange("Will this week be kind?")
            viewModel.ask()
            advanceUntilIdle()

            assertEquals("en", repo.lastRequest?.lang)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `ask success refreshes economy backend snapshot`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeOracleRepository(
                scriptedResults = listOf(
                    ApiResult.Ok(
                        OracleAskResult.InProgress(
                            requestId = "req-1",
                            status = "IN_PROGRESS",
                        ),
                    ),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.English))
            val economyRepository = FakeEconomyRepository()
            val analytics = FakeAnalyticsTracker()
            val viewModel = OracleAskViewModel(
                oracleRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                economyRepository = economyRepository,
                analyticsTracker = analytics,
            )

            viewModel.onQuestionChange("How should I focus today?")
            viewModel.ask()
            advanceUntilIdle()

            assertEquals(1, economyRepository.getStatusCalls)
            assertNull(viewModel.uiState.value.error)
            assertEquals(10, viewModel.uiState.value.economyBalance)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `ask economy restriction error refreshes economy and does not map as legacy ad unlock`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeOracleRepository(
                scriptedResults = listOf(
                    ApiResult.Err(ApiError.FailedPrecondition("insufficient_moons for oracle")),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.English))
            val economyRepository = FakeEconomyRepository()
            val analytics = FakeAnalyticsTracker()
            val viewModel = OracleAskViewModel(
                oracleRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                economyRepository = economyRepository,
                analyticsTracker = analytics,
            )

            viewModel.onQuestionChange("Will this week be kind?")
            viewModel.ask()
            advanceUntilIdle()

            assertEquals(1, economyRepository.getStatusCalls)
            assertEquals(
                OracleAskMessageId.FailedPreconditionGeneric,
                viewModel.uiState.value.error?.id,
            )
            assertEquals(10, viewModel.uiState.value.economyBalance)
            assertTrue(analytics.events.any { it is com.agc.bwitch.domain.analytics.AnalyticsEvent.ModuleLimitReached })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `ask legacy ad unlock signal keeps temporary compatibility without economy refresh`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeOracleRepository(
                scriptedResults = listOf(
                    ApiResult.Err(ApiError.FailedPrecondition("AD_UNLOCK rewardedProof required")),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.English))
            val economyRepository = FakeEconomyRepository()
            val viewModel = OracleAskViewModel(
                oracleRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                economyRepository = economyRepository,
            )

            viewModel.onQuestionChange("Will this week be kind?")
            viewModel.ask()
            advanceUntilIdle()

            assertEquals(0, economyRepository.getStatusCalls)
            assertEquals(
                OracleAskMessageId.FailedPreconditionWithAdUnlock,
                viewModel.uiState.value.error?.id,
            )
            assertNull(viewModel.uiState.value.economyBalance)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeOracleRepository : OracleRepository {
        constructor(scriptedResults: List<ApiResult<OracleAskResult>> = emptyList()) {
            results.addAll(scriptedResults)
        }

        private val results = ArrayDeque<ApiResult<OracleAskResult>>()
        var lastRequest: OracleAskRequest? = null

        override suspend fun getStatus() = ApiResult.Err(com.agc.bwitch.domain.shared.ApiError.Unknown())

        override suspend fun ask(request: OracleAskRequest): ApiResult<OracleAskResult> {
            lastRequest = request
            return results.removeFirstOrNull()
                ?: ApiResult.Ok(
                    OracleAskResult.InProgress(
                        requestId = request.requestId,
                        status = "IN_PROGRESS",
                    ),
                )
        }
    }

    private class FakeLanguageRepository(
        private val state: MutableStateFlow<AppLanguage>,
    ) : AppLanguageRepository {
        override suspend fun resolveCurrentLanguage(): AppLanguage = state.value
        override suspend fun getCurrentLanguage(): AppLanguage = state.value
        override suspend fun setCurrentLanguage(language: AppLanguage) {
            state.value = language
        }
        override fun observeCurrentLanguage(): Flow<AppLanguage> = state
    }

    private class FakeEconomyRepository : EconomyRepository {
        var getStatusCalls: Int = 0
        var getBalanceCalls: Int = 0

        override suspend fun getBalance(): EconomyBalance {
            getBalanceCalls += 1
            return EconomyBalance(
                balance = 10,
                dailyLoginClaimed = false,
                rewardedAdsClaimed = 0,
                rewardedAdsRemaining = 1,
            )
        }

        override suspend fun getStatus(): EconomyStatus {
            getStatusCalls += 1
            return EconomyStatus(
                balance = 10,
                isPremium = false,
                todayDateIso = "2026-01-01",
            )
        }

        override suspend fun claimDailyLogin(requestId: String): EconomyClaimResult = EconomyClaimResult(
            result = EconomyClaimStatus.CLAIMED,
            balance = 11,
            dailyLoginClaimed = true,
            rewardedAdsClaimed = 0,
            rewardedAdsRemaining = 1,
        )

        override suspend fun claimRewardedAd(
            requestId: String,
            adProof: String,
            placement: String?,
        ): EconomyClaimResult = EconomyClaimResult(
            result = EconomyClaimStatus.CLAIMED,
            balance = 11,
            dailyLoginClaimed = false,
            rewardedAdsClaimed = 1,
            rewardedAdsRemaining = 0,
        )
    }
}
