package com.agc.bwitch.domain.astrology.horoscope

import com.agc.bwitch.domain.model.ApiResult

class GetDailyHoroscopeUseCase(
    private val repository: HoroscopeRepository,
) {
    suspend operator fun invoke(sign: ZodiacSign, dateIso: String? = null): ApiResult<DailyHoroscope> {
        return repository.getDaily(sign = sign, dateIso = dateIso)
    }
}
