package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.coroutines.runBlocking
class GetDailyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    suspend operator fun invoke(dateIso: String, sign: ZodiacSign): DailyHoroscope? {
        return repository.getDaily(dateIso = dateIso, sign = sign)
    }
}
