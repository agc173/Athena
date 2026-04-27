package com.agc.bwitch.domain.astrology.horoscope

class IsHoroscopeDayUnlockedUseCase(
    private val repository: HoroscopeUnlockRepository,
) {
    suspend operator fun invoke(dateIso: String): Boolean {
        return repository.isUnlocked(dateIso = dateIso)
    }

    suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> {
        return repository.getUnlockedDays(dateIsoList = dateIsoList)
    }
}
