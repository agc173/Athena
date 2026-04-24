package com.agc.bwitch.domain.astrology.horoscope

class UnlockHoroscopeFutureDayUseCase(
    private val repository: HoroscopeUnlockRepository,
) {
    suspend operator fun invoke(dateIso: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
        return repository.unlockFutureDay(dateIso = dateIso, requestId = requestId, sign = sign)
    }
}
