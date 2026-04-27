package com.agc.bwitch.domain.astrology.horoscope

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class GetDailyHoroscopeUseCaseTest {

    private val repo = FakeHoroscopeRepository()
    private val useCase = GetDailyHoroscopeUseCase(repo)

    @Test
    fun returns_null_when_repo_has_no_value() = runBlocking {
        repo.value = null

        val result = useCase(
            dateIso = "2026-02-25",
            sign = ZodiacSign.aries,
            languageCode = "es",
        )

        assertNull(result)
    }

    @Test
    fun returns_value_when_repo_has_value() = runBlocking {
        val expected = DailyHoroscope(
            sign = ZodiacSign.aries,
            dateIso = "2026-02-25",
            languageCode = "es",
            text = "Hola",
            mood = "Bien",
            luckyNumber = 7,
            luckyColor = "Rojo",
            shareText = null,
        )
        repo.value = expected

        val result = useCase(
            dateIso = "2026-02-25",
            sign = ZodiacSign.aries,
            languageCode = "es",
        )

        assertEquals(expected, result)
    }

    private class FakeHoroscopeRepository : HoroscopeRepository {
        var value: DailyHoroscope? = null

        override fun observeDaily(dateIso: String, sign: ZodiacSign, languageCode: String): Flow<DailyHoroscope?> {
            return flowOf(value)
        }

        override suspend fun getDaily(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? {
            return value
        }

        override fun observeWeekly(weekKey: String, sign: ZodiacSign, languageCode: String): Flow<WeeklyHoroscope?> =
            flowOf(null)

        override suspend fun getWeekly(weekKey: String, sign: ZodiacSign, languageCode: String): WeeklyHoroscope? =
            null

        override fun observeMonthly(monthKey: String, sign: ZodiacSign, languageCode: String): Flow<MonthlyHoroscope?> =
            flowOf(null)

        override suspend fun getMonthly(monthKey: String, sign: ZodiacSign, languageCode: String): MonthlyHoroscope? =
            null
    }
}
