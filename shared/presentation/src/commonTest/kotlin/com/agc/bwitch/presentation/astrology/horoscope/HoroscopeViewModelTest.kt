package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PrefetchDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class HoroscopeViewModelTest {

    @Test
    fun initLoadsDefaultSignAndHoroscopeIsNotNull() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())

        val prefetchUseCase = PrefetchDailyHoroscopeUseCase(
            syncController = FakeSync(),
            clock = Clock.System
        )

        // Pre-cargamos el valor que emitirá observe()
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-02-25",
                text = "Texto para Aries",
                mood = "Positivo",
                luckyNumber = 7,
                luckyColor = "Azul",
            )
        )

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            prefetchDailyHoroscopeUseCase = prefetchUseCase,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.horoscope)
        assertNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun onSelectSignUpdatesSelectedSignAndHoroscope() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())

        val prefetchUseCase = PrefetchDailyHoroscopeUseCase(
            syncController = FakeSync(),
            clock = Clock.System
        )

        // Inicial: aries
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-02-25",
                text = "Texto Aries",
                mood = "Positivo",
                luckyNumber = 7,
                luckyColor = "Azul",
            )
        )

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            prefetchDailyHoroscopeUseCase = prefetchUseCase,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        // Cambiamos signo y emitimos el nuevo valor
        viewModel.onSelectSign(ZodiacSign.leo)
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.leo,
                dateIso = "2026-02-25",
                text = "Texto Leo",
                mood = "Fuerte",
                luckyNumber = 1,
                luckyColor = "Rojo",
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ZodiacSign.leo, state.selectedSign)
        assertEquals(ZodiacSign.leo, state.horoscope?.sign)
        assertNull(state.errorMessage)
    }

    @Test
    fun errorMessageRemainsNullIfPrefetchFailsSilently() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)

        // Pull manual (onRefresh) no se llama en este test, pero lo dejamos no-op
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())

        // Prefetch que falla (init), pero en el VM lo tratamos "silencioso"
        val prefetchUseCase = PrefetchDailyHoroscopeUseCase(
            syncController = FailingSync(),
            clock = Clock.System
        )

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            prefetchDailyHoroscopeUseCase = prefetchUseCase,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.errorMessage)
    }

    private class FakeRepo : HoroscopeRepository {
        private val state = MutableStateFlow<DailyHoroscope?>(null)

        fun emit(value: DailyHoroscope?) {
            state.value = value
        }

        override fun observeDaily(dateIso: String, sign: ZodiacSign): Flow<DailyHoroscope?> {
            // Para simplificar el test, ignoramos filtros (date/sign) y emitimos lo que toque.
            return state.asStateFlow()
        }

        override suspend fun getDaily(dateIso: String, sign: ZodiacSign): DailyHoroscope? {
            return state.value
        }
    }

    private class FakeSync : HoroscopeDailySyncController {
        override suspend fun pull(dateIso: String) {
            // NO-OP
        }
    }

    private class FailingSync : HoroscopeDailySyncController {
        override suspend fun pull(dateIso: String) {
            throw RuntimeException("boom")
        }
    }
}
