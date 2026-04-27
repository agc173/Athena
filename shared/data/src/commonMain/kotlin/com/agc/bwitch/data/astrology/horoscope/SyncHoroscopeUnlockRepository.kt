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

    override suspend fun isUnlocked(dateIso: String): Boolean {
        return getUnlockedDays(dateIsoList = listOf(dateIso)).contains(dateIso)
    }

    override suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> {
        return runCatching {
            remoteDataSource.getHoroscopeDailyUnlocks(dateIsoList)
        }.onSuccess {
            println("[SyncHoroscopeUnlockRepository] getUnlockedDays(input=$dateIsoList) unlocked=$it")
        }.onFailure {
            println("[SyncHoroscopeUnlockRepository] getUnlockedDays(input=$dateIsoList) failed: ${it.message}")
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

}
