package com.agc.bwitch.domain.astrology.horoscope

interface HoroscopeUnlockRepository {
    suspend fun getFutureDayCost(): Int
    suspend fun isUnlocked(dateIso: String): Boolean
    suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> =
        dateIsoList.filterTo(mutableSetOf()) { dateIso -> isUnlocked(dateIso) }
    suspend fun unlockFutureDay(dateIso: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult
}

data class HoroscopeUnlockResult(
    val unlocked: Boolean,
    val alreadyUnlocked: Boolean,
    val balanceAfter: Int,
    val costCharged: Int,
)
