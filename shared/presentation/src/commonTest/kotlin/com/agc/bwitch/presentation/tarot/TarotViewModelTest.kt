package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.moons.AddMoonsUseCase
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.moons.SpendMoonsResult
import com.agc.bwitch.domain.moons.SpendMoonsUseCase
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TarotViewModelTest {

    @Test
    fun `newRequest uses current app language for backend draw`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository()
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.German))
            val moonRepository = FakeMoonRepository()
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                spendMoonsUseCase = SpendMoonsUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
            )

            viewModel.newRequest(TarotRequestType.TAROT_1)
            advanceUntilIdle()

            assertEquals("de", repo.lastLang)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `retry for tarot_3 reuses newRequest flow and spends moons again`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository()
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                spendMoonsUseCase = SpendMoonsUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
            )

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(2, moonRepository.spendCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeMoonRepository(initialBalance: Int = 0) : MoonRepository {
        private val state = MutableStateFlow(MoonBalance(initialBalance))
        var spendCalls: Int = 0
        override suspend fun getBalance(): MoonBalance = state.value
        override fun observeBalance(): Flow<MoonBalance> = state
        override suspend fun addMoons(amount: Int): MoonBalance = state.value
        override suspend fun spendMoons(amount: Int): SpendMoonsResult {
            spendCalls += 1
            val current = state.value
            return if (current.amount < amount) {
                SpendMoonsResult.InsufficientBalance(currentBalance = current, required = amount)
            } else {
                val updated = MoonBalance(current.amount - amount)
                state.value = updated
                SpendMoonsResult.Success(updated)
            }
        }
        override suspend fun hasEnough(amount: Int): Boolean = true
    }

    private class FakeTarotRepository : TarotRepository {
        var lastLang: String? = null

        override suspend fun tarotDraw(
            requestId: String,
            type: TarotRequestType,
            lang: String?,
            question: String?,
        ): ApiResult<TarotDrawResponse> {
            lastLang = lang
            return ApiResult.Err(ApiError.Internal("mock error"))
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
