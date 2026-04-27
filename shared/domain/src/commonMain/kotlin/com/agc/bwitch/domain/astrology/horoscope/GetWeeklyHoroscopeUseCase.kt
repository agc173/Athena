package com.agc.bwitch.domain.astrology.horoscope

class GetWeeklyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    suspend operator fun invoke(weekKey: String, sign: ZodiacSign, languageCode: String): WeeklyHoroscope? {
        return repository.getWeekly(weekKey = weekKey, sign = sign, languageCode = languageCode)
    }
}
