package com.agc.bwitch.domain.astrology.horoscope

class ObserveWeeklyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    operator fun invoke(weekKey: String, sign: ZodiacSign, languageCode: String) =
        repository.observeWeekly(weekKey = weekKey, sign = sign, languageCode = languageCode)
}
