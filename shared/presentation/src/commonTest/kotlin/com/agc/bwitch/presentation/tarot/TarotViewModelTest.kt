package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.moons.AddMoonsUseCase
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.moons.SpendMoonsResult
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.tarot.GetSelectedTarotDeckUseCase
import com.agc.bwitch.domain.tarot.GetTarotDeckCollectionProgressUseCase
import com.agc.bwitch.domain.tarot.SelectedTarotDeckRepository
import com.agc.bwitch.domain.tarot.TarotDeckCollectionProgress
import com.agc.bwitch.domain.tarot.TarotDeckCollectionRepository
import com.agc.bwitch.domain.tarot.TarotDeckId
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.LastTarotReadingRepository
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.presentation.analytics.FakeAnalyticsTracker
import kotlin.collections.ArrayDeque
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = FakeLastTarotReadingRepository(),
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(FakeSelectedTarotDeckRepository()),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(FakeTarotDeckCollectionRepository()),
            )

            viewModel.newRequest(TarotRequestType.TAROT_1)
            advanceUntilIdle()

            assertEquals("de", repo.lastLang)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `retry for tarot_3 reuses newRequest flow without local upfront spend`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository()
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val analytics = FakeAnalyticsTracker()
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = FakeLastTarotReadingRepository(),
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(FakeSelectedTarotDeckRepository()),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(FakeTarotDeckCollectionRepository()),
                analyticsTracker = analytics,
            )

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()
            viewModel.retry()
            advanceUntilIdle()

            assertEquals(0, moonRepository.spendCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `tarot_3 legacy rewarded proof error does not spend locally and does not auto retry`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository(
                scriptedResults = listOf(
                    ApiResult.Err(ApiError.FailedPrecondition("AD_UNLOCK rewardedProof required")),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val analytics = FakeAnalyticsTracker()
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = FakeLastTarotReadingRepository(),
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(FakeSelectedTarotDeckRepository()),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(FakeTarotDeckCollectionRepository()),
                analyticsTracker = analytics,
            )

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()

            assertEquals(1, repo.callCount)
            assertEquals(0, moonRepository.spendCalls)
            assertEquals(TAROT_DRAW_ERROR_KEY, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.insufficientMoonsMessage)
            assertNull(viewModel.uiState.value.response)
            assertEquals(10, viewModel.uiState.value.moonBalance)
            assertTrue(analytics.events.none { it is com.agc.bwitch.domain.analytics.AnalyticsEvent.ContentUnlocked })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `tarot_3 quota error does not map to insufficient moons`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository(
                scriptedResults = listOf(
                    ApiResult.Err(ApiError.ResourceExhausted("daily quota exceeded")),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = FakeLastTarotReadingRepository(),
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(FakeSelectedTarotDeckRepository()),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(FakeTarotDeckCollectionRepository()),
            )

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()

            assertEquals(TAROT_DRAW_ERROR_KEY, viewModel.uiState.value.error)
            assertNull(viewModel.uiState.value.insufficientMoonsMessage)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `tarot_3 backend refresh does not apply local delta adjustments`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository(
                scriptedResults = listOf(
                    ApiResult.Ok(
                        TarotDrawResponse(
                            requestId = "req-1",
                            status = "DONE",
                            cards = emptyList(),
                            interpretation = "ok",
                        ),
                    ),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = FakeLastTarotReadingRepository(),
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(FakeSelectedTarotDeckRepository()),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(FakeTarotDeckCollectionRepository()),
            )

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()

            assertEquals(0, moonRepository.spendCalls)
            assertEquals(0, moonRepository.addCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `tarot error from tarot_1 is cleared and does not contaminate tarot_3`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository(
                scriptedResults = listOf(
                    ApiResult.Err(ApiError.Internal("tarot1 failure")),
                    ApiResult.Ok(
                        TarotDrawResponse(
                            requestId = "req-2",
                            status = "DONE",
                            cards = emptyList(),
                            interpretation = "ok tarot3",
                        ),
                    ),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = FakeLastTarotReadingRepository(),
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(FakeSelectedTarotDeckRepository()),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(FakeTarotDeckCollectionRepository()),
            )

            viewModel.newRequest(TarotRequestType.TAROT_1)
            advanceUntilIdle()
            assertEquals(TAROT_DRAW_ERROR_KEY, viewModel.uiState.value.error)

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.error)
            assertEquals("DONE", viewModel.uiState.value.response?.status)
        } finally {
            Dispatchers.resetMain()
        }
    }


    @Test
    fun `new request resolves playable deck deterministically before draw`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository(
                scriptedResults = listOf(
                    ApiResult.Ok(
                        TarotDrawResponse(
                            requestId = "req-1",
                            status = "DONE",
                            cards = emptyList(),
                            interpretation = "ok",
                            deckId = TarotDeckId.ARCANA_NOCTIS,
                        ),
                    ),
                ),
            )
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
            val moonRepository = FakeMoonRepository(initialBalance = 10)
            val lastReadingRepository = FakeLastTarotReadingRepository()
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
                observeMoonBalanceUseCase = ObserveMoonBalanceUseCase(moonRepository),
                getMoonBalanceUseCase = GetMoonBalanceUseCase(moonRepository),
                addMoonsUseCase = AddMoonsUseCase(moonRepository),
                lastTarotReadingRepository = lastReadingRepository,
                getSelectedTarotDeckUseCase = GetSelectedTarotDeckUseCase(
                    FakeSelectedTarotDeckRepository(TarotDeckId.ARCANA_NOCTIS),
                ),
                getTarotDeckCollectionProgressUseCase = GetTarotDeckCollectionProgressUseCase(
                    FakeTarotDeckCollectionRepository(),
                ),
            )

            viewModel.newRequest(TarotRequestType.TAROT_3)
            advanceUntilIdle()

            assertEquals(TarotDeckId.RIDER_WAITE, viewModel.uiState.value.selectedDeckId)
            assertEquals(TarotDeckId.RIDER_WAITE, viewModel.uiState.value.response?.deckId)
            assertEquals(TarotDeckId.RIDER_WAITE, lastReadingRepository.get()?.deckId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeMoonRepository(initialBalance: Int = 0) : MoonRepository {
        private val state = MutableStateFlow(MoonBalance(initialBalance))
        var spendCalls: Int = 0
        var addCalls: Int = 0

        override suspend fun getBalance(): MoonBalance = state.value

        override fun observeBalance(): Flow<MoonBalance> = state

        override suspend fun addMoons(amount: Int): MoonBalance {
            addCalls += 1
            val updated = MoonBalance(state.value.amount + amount)
            state.value = updated
            return updated
        }

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

    private class FakeTarotRepository(
        scriptedResults: List<ApiResult<TarotDrawResponse>> = emptyList(),
    ) : TarotRepository {
        private val results = ArrayDeque(scriptedResults)
        var lastLang: String? = null
        var callCount: Int = 0

        override suspend fun tarotDraw(
            requestId: String,
            type: TarotRequestType,
            lang: String?,
            question: String?,
        ): ApiResult<TarotDrawResponse> {
            callCount += 1
            lastLang = lang
            return results.removeFirstOrNull()
                ?: ApiResult.Err(ApiError.Internal("mock error"))
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

    private class FakeLastTarotReadingRepository : LastTarotReadingRepository {
        private var cached: TarotDrawResponse? = null
        override fun get(): TarotDrawResponse? = cached
        override fun save(response: TarotDrawResponse) {
            cached = response
        }
    }

    private class FakeSelectedTarotDeckRepository(
        private val selectedDeckId: TarotDeckId = TarotDeckId.RIDER_WAITE,
    ) : SelectedTarotDeckRepository {
        override fun getSelectedDeckId(): TarotDeckId = selectedDeckId
        override fun setSelectedDeckId(deckId: TarotDeckId) = Unit
    }

    private class FakeTarotDeckCollectionRepository(
        private val progress: Map<String, TarotDeckCollectionProgress> = emptyMap(),
    ) : TarotDeckCollectionRepository {
        override suspend fun getProgressByTrackId(): Map<String, TarotDeckCollectionProgress> = progress
    }

}
