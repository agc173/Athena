package com.agc.bwitch.domain.astrology.horoscope

class ObserveDailyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    operator fun invoke(dateIso: String, sign: ZodiacSign) =
        repository.observeDaily(dateIso = dateIso, sign = sign)
}