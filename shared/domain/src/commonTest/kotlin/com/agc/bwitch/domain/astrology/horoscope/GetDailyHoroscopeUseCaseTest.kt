package com.agc.bwitch.domain.astrology.horoscope

import com.agc.bwitch.domain.model.ApiResult
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GetDailyHoroscopeUseCaseTest {

    @Test
    fun returnsSuccessWhenRepositoryReturnsSuccess() {
        val expected = DailyHoroscope(
            sign = ZodiacSign.aries,
            dateIso = "2026-02-18",
            text = "Hoy es un gran día para iniciar algo nuevo.",
            mood = "Energético",
            luckyNumber = 7,
            luckyColor = "Rojo",
            shareText = null,
        )
        val useCase = GetDailyHoroscopeUseCase(
            repository = FakeHoroscopeRepository(
                result = ApiResult.Success(expected),
            ),
        )

        val result = runSuspend { useCase(sign = ZodiacSign.aries) }

        val success = assertIs<ApiResult.Success<DailyHoroscope>>(result)
        assertEquals(expected, success.data)
    }

    @Test
    fun propagatesSignToRepository() {
        val fakeRepository = FakeHoroscopeRepository(
            result = ApiResult.Success(
                DailyHoroscope(
                    sign = ZodiacSign.pisces,
                    dateIso = "2026-02-18",
                    text = "Confía en tu intuición.",
                    mood = "Reflexivo",
                    luckyNumber = 12,
                    luckyColor = "Turquesa",
                ),
            ),
        )
        val useCase = GetDailyHoroscopeUseCase(repository = fakeRepository)

        runSuspend { useCase(sign = ZodiacSign.pisces) }

        assertEquals(ZodiacSign.pisces, fakeRepository.lastSign)
    }

    private class FakeHoroscopeRepository(
        private val result: ApiResult<DailyHoroscope>,
    ) : HoroscopeRepository {
        var lastSign: ZodiacSign? = null
            private set

        override suspend fun getDaily(sign: ZodiacSign, dateIso: String?): ApiResult<DailyHoroscope> {
            lastSign = sign
            return result
        }
    }

    private fun <T> runSuspend(block: suspend () -> T): T {
        var completed: Result<T>? = null
        block.startCoroutine(object : Continuation<T> {
            override val context: CoroutineContext = EmptyCoroutineContext

            override fun resumeWith(result: Result<T>) {
                completed = result
            }
        })
        val finalResult = checkNotNull(completed) { "Suspend block did not complete" }
        return finalResult.getOrThrow()
    }
}
