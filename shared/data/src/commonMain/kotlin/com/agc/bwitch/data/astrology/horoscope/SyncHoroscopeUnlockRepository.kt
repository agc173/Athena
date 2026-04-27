package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.data.remote.economy.EconomyRemoteDataSource
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockResult
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.auth.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class SyncHoroscopeUnlockRepository(
    private val remoteDataSource: EconomyRemoteDataSource,
    private val authRepository: AuthRepository,
) : HoroscopeUnlockRepository {

    private val firestore = Firebase.firestore

    override suspend fun getFutureDayCost(): Int {
        return runCatching { remoteDataSource.getStatus().rules?.horoscope?.costs?.futureDay ?: 1 }
            .getOrDefault(1)
    }

    override suspend fun isUnlocked(dateIso: String): Boolean {
        val uid = currentUidOrNull()
        if (uid == null) {
            println("[SyncHoroscopeUnlockRepository] isUnlocked(dateIso=$dateIso) uid=null -> false")
            return false
        }
        val unlockKey = unlockKey(dateIso)
        val exists = firestore.collection("economyUnlocks")
            .document(uid)
            .collection("horoscope")
            .document(unlockKey)
            .get()
            .exists
        println("[SyncHoroscopeUnlockRepository] isUnlocked(dateIso=$dateIso uid=$uid unlockKey=$unlockKey path=${economyHoroscopeUnlockPath(uid, unlockKey)}) result=$exists")
        return exists
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

    private suspend fun currentUidOrNull(): String? =
        withTimeoutOrNull(5_000) {
            authRepository.authState
                .filterNotNull()
                .first()
                .uid
        }

    private fun unlockKey(dateIso: String): String = "daily:$dateIso"

    private fun economyHoroscopeUnlockPath(uid: String, unlockKey: String): String =
        "economyUnlocks/$uid/horoscope/$unlockKey"
}
