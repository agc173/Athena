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
        val uid = currentUidOrNull() ?: return false
        return firestore.collection("economyUnlocks")
            .document(uid)
            .collection("horoscope")
            .document(unlockKey(dateIso))
            .get()
            .exists
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
        withTimeoutOrNull(2_000) {
            authRepository.authState
                .filterNotNull()
                .first()
                .uid
        }

    private fun unlockKey(dateIso: String): String = "daily:$dateIso"
}
