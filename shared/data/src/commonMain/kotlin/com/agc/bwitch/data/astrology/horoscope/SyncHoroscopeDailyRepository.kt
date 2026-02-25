package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

class SyncHoroscopeDailyRepository(
    private val local: SettingsHoroscopeDailyRepository,
) : HoroscopeRepository, HoroscopeDailySyncController {

    private val firestore = Firebase.firestore

    override fun observeDaily(dateIso: String, sign: ZodiacSign): Flow<DailyHoroscope?> =
        local.observeDaily(dateIso, sign)

    override suspend fun getDaily(dateIso: String, sign: ZodiacSign): DailyHoroscope? =
        local.getDaily(dateIso, sign)

    override suspend fun pull(dateIso: String) {
        // Pull de todos los signos para esa fecha (12 docs aprox)
        ZodiacSign.entries.forEach { sign ->
            val remote = fetchRemote(dateIso = dateIso, sign = sign) ?: return@forEach

            val localValue = local.getDaily(dateIso = dateIso, sign = sign)
            val localUpdated = localValue?.updatedAtEpochMillis ?: 0L

            if (remote.updatedAtEpochMillis > localUpdated) {
                local.saveDaily(remote)
            }
        }
    }

    private fun doc(dateIso: String, sign: ZodiacSign) =
        firestore.collection("horoscopeDaily")
            .document(dateIso)
            .collection("signs")
            .document(sign.name)

    private suspend fun fetchRemote(dateIso: String, sign: ZodiacSign): DailyHoroscope? {
        val snap = doc(dateIso, sign).get()
        if (!snap.exists) return null

        val dto = snap.data(HoroscopeDailyRemoteDto.serializer())
        return dto.toDomain(sign = sign, dateIso = dateIso)
    }
}

@Serializable
data class HoroscopeDailyRemoteDto(
    val text: String,
    val mood: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val shareText: String? = null,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain(sign: ZodiacSign, dateIso: String): DailyHoroscope =
        DailyHoroscope(
            sign = sign,
            dateIso = dateIso,
            text = text,
            mood = mood,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            shareText = shareText,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}