package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.model.ApiResult
import com.agc.bwitch.domain.model.NetworkError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class HoroscopeViewModelTest {

    @Test
    fun initLoadsAriesAndHoroscopeIsNotNull() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = HoroscopeViewModel(
            getDailyHoroscopeUseCase = GetDailyHoroscopeUseCase(SuccessRepo()),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ZodiacSign.aries, state.selectedSign)
        assertNotNull(state.horoscope)
        assertNull(state.errorMessage)
    }

    @Test
    fun onSelectSignUpdatesSelectedSignAndHoroscope() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = HoroscopeViewModel(
            getDailyHoroscopeUseCase = GetDailyHoroscopeUseCase(SuccessRepo()),
            dispatcher = dispatcher,
        )

        viewModel.onSelectSign(ZodiacSign.leo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ZodiacSign.leo, state.selectedSign)
        assertEquals(ZodiacSign.leo, state.horoscope?.sign)
    }

    @Test
    fun returnsErrorMessageWhenUseCaseFails() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val viewModel = HoroscopeViewModel(
            getDailyHoroscopeUseCase = GetDailyHoroscopeUseCase(FailureRepo()),
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
    }

    private class SuccessRepo : HoroscopeRepository {
        override suspend fun getDaily(sign: ZodiacSign, dateIso: String?): ApiResult<DailyHoroscope> {
            return ApiResult.Success(
                DailyHoroscope(
                    sign = sign,
                    dateIso = dateIso ?: "2026-02-18",
                    text = "Texto para ${sign.label}",
                    mood = "Positivo",
                    luckyNumber = 7,
                    luckyColor = "Azul",
                ),
            )
        }
    }

    private class FailureRepo : HoroscopeRepository {
        override suspend fun getDaily(sign: ZodiacSign, dateIso: String?): ApiResult<DailyHoroscope> {
            return ApiResult.Failure(NetworkError.Unknown)
        }
    }
}
