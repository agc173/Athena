package com.agc.bwitch.domain.astrology.horoscope

class GetHoroscopeFutureDayCostUseCase(
    private val repository: HoroscopeUnlockRepository,
) {
    suspend operator fun invoke(): Int = repository.getFutureDayCost()
}
