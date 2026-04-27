package com.agc.bwitch.domain.astrology.horoscope

class GetMonthlyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    suspend operator fun invoke(monthKey: String, sign: ZodiacSign, languageCode: String): MonthlyHoroscope? {
        return repository.getMonthly(monthKey = monthKey, sign = sign, languageCode = languageCode)
    }
}
