package com.agc.bwitch.presentation.oracle

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.oracle.OracleAskResult
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
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
            )

            viewModel.onQuestionChange("Will this week be kind?")
            viewModel.ask()
            advanceUntilIdle()

            assertEquals("en", repo.lastRequest?.lang)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeOracleRepository : OracleRepository {
        var lastRequest: OracleAskRequest? = null

        override suspend fun getStatus() = ApiResult.Err(com.agc.bwitch.domain.shared.ApiError.Unknown())

        override suspend fun ask(request: OracleAskRequest): ApiResult<OracleAskResult> {
            lastRequest = request
            return ApiResult.Ok(
                OracleAskResult.InProgress(
                    requestId = request.requestId,
                    status = "IN_PROGRESS",
                )
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
}
