package com.agc.bwitch.domain.astrology.horoscope

class GetDailyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    suspend operator fun invoke(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? {
        return repository.getDaily(dateIso = dateIso, sign = sign, languageCode = languageCode)
    }
}
