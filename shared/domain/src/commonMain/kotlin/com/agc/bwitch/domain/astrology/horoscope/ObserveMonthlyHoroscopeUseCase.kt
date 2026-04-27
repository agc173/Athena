package com.agc.bwitch.domain.astrology.horoscope

class ObserveMonthlyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    operator fun invoke(monthKey: String, sign: ZodiacSign, languageCode: String) =
        repository.observeMonthly(monthKey = monthKey, sign = sign, languageCode = languageCode)
}
