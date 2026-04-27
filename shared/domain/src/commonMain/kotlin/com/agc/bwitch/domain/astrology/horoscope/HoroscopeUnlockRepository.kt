package com.agc.bwitch.domain.astrology.horoscope

interface HoroscopeUnlockRepository {
    suspend fun getFutureDayCost(): Int
    suspend fun getWeeklyCost(): Int = getFutureDayCost()
    suspend fun getMonthlyCost(): Int = getFutureDayCost()
    suspend fun isUnlocked(dateIso: String): Boolean
    suspend fun isWeekUnlocked(weekKey: String): Boolean = false
    suspend fun isMonthUnlocked(monthKey: String): Boolean = false
    suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> =
        dateIsoList.filterTo(mutableSetOf()) { dateIso -> isUnlocked(dateIso) }
    suspend fun getUnlockedWeeks(weekKeyList: List<String>): Set<String> =
        weekKeyList.filterTo(mutableSetOf()) { weekKey -> isWeekUnlocked(weekKey) }
    suspend fun getUnlockedMonths(monthKeyList: List<String>): Set<String> =
        monthKeyList.filterTo(mutableSetOf()) { monthKey -> isMonthUnlocked(monthKey) }
    suspend fun unlockFutureDay(dateIso: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult
    suspend fun unlockWeek(weekKey: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult =
        HoroscopeUnlockResult(unlocked = false, alreadyUnlocked = false, balanceAfter = 0, costCharged = 0)
    suspend fun unlockMonth(monthKey: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult =
        HoroscopeUnlockResult(unlocked = false, alreadyUnlocked = false, balanceAfter = 0, costCharged = 0)
}

data class HoroscopeUnlockResult(
    val unlocked: Boolean,
    val alreadyUnlocked: Boolean,
    val balanceAfter: Int,
    val costCharged: Int,
)
