package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.data.remote.economy.EconomyRemoteDataSource
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockResult
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

class SyncHoroscopeUnlockRepository(
    private val remoteDataSource: EconomyRemoteDataSource,
) : HoroscopeUnlockRepository {

    override suspend fun getFutureDayCost(): Int {
        return runCatching { remoteDataSource.getStatus().rules?.horoscope?.costs?.futureDay ?: 1 }
            .getOrDefault(1)
    }

    override suspend fun getWeeklyCost(): Int {
        return runCatching { remoteDataSource.getStatus().rules?.horoscope?.costs?.weekly ?: 1 }
            .getOrDefault(1)
    }

    override suspend fun getMonthlyCost(): Int {
        return runCatching { remoteDataSource.getStatus().rules?.horoscope?.costs?.monthly ?: 1 }
            .getOrDefault(1)
    }

    override suspend fun isUnlocked(dateIso: String): Boolean {
        return getUnlockedDays(dateIsoList = listOf(dateIso)).contains(dateIso)
    }

    override suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> {
        return runCatching {
            remoteDataSource.getHoroscopeDailyUnlocks(dateIsoList)
        }.getOrDefault(emptySet())
    }

    override suspend fun unlockFutureDay(dateIso: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
        val response = remoteDataSource.unlockHoroscopeDay(
            requestId = requestId,
            dateIso = dateIso,
            sign = sign.name,
        )

        return HoroscopeUnlockResult(
            unlocked = response.unlocked,
            alreadyUnlocked = response.alreadyUnlocked,
            balanceAfter = response.balance,
            costCharged = response.costCharged,
        )
    }

    override suspend fun isWeekUnlocked(weekKey: String): Boolean {
        return getUnlockedWeeks(weekKeyList = listOf(weekKey)).contains(weekKey)
    }

    override suspend fun getUnlockedWeeks(weekKeyList: List<String>): Set<String> {
        return runCatching { remoteDataSource.getHoroscopeWeeklyUnlocks(weekKeyList) }.getOrDefault(emptySet())
    }

    override suspend fun unlockWeek(weekKey: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
        val response = remoteDataSource.unlockHoroscopeWeek(
            requestId = requestId,
            weekKey = weekKey,
            sign = sign.name,
        )
        return HoroscopeUnlockResult(
            unlocked = response.unlocked,
            alreadyUnlocked = response.alreadyUnlocked,
            balanceAfter = response.balance,
            costCharged = response.costCharged,
        )
    }

    override suspend fun isMonthUnlocked(monthKey: String): Boolean {
        return getUnlockedMonths(monthKeyList = listOf(monthKey)).contains(monthKey)
    }

    override suspend fun getUnlockedMonths(monthKeyList: List<String>): Set<String> {
        return runCatching { remoteDataSource.getHoroscopeMonthlyUnlocks(monthKeyList) }.getOrDefault(emptySet())
    }

    override suspend fun unlockMonth(monthKey: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
        val response = remoteDataSource.unlockHoroscopeMonth(
            requestId = requestId,
            monthKey = monthKey,
            sign = sign.name,
        )
        return HoroscopeUnlockResult(
            unlocked = response.unlocked,
            alreadyUnlocked = response.alreadyUnlocked,
            balanceAfter = response.balance,
            costCharged = response.costCharged,
        )
    }

}
