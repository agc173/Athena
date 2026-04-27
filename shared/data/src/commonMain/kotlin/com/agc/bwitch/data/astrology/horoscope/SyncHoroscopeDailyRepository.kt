package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.domain.astrology.horoscope.MonthlyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.WeeklyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.auth.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable

class SyncHoroscopeDailyRepository(
    private val local: SettingsHoroscopeDailyRepository,
    private val authRepository: AuthRepository,
) : HoroscopeRepository, HoroscopeDailySyncController {

    private val firestore = Firebase.firestore

    override fun observeDaily(dateIso: String, sign: ZodiacSign, languageCode: String): Flow<DailyHoroscope?> =
        local.observeDaily(dateIso, sign, languageCode)

    override suspend fun getDaily(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? =
        local.getDaily(dateIso, sign, languageCode)

    override fun observeWeekly(weekKey: String, sign: ZodiacSign, languageCode: String): Flow<WeeklyHoroscope?> =
        flow { emit(getWeekly(weekKey, sign, languageCode)) }

    override suspend fun getWeekly(weekKey: String, sign: ZodiacSign, languageCode: String): WeeklyHoroscope? {
        return fetchWeeklyRemote(weekKey = weekKey, sign = sign, languageCode = languageCode)
    }

    override fun observeMonthly(monthKey: String, sign: ZodiacSign, languageCode: String): Flow<MonthlyHoroscope?> =
        flow { emit(getMonthly(monthKey, sign, languageCode)) }

    override suspend fun getMonthly(monthKey: String, sign: ZodiacSign, languageCode: String): MonthlyHoroscope? {
        return fetchMonthlyRemote(monthKey = monthKey, sign = sign, languageCode = languageCode)
    }

    override suspend fun pull(dateIso: String, languageCode: String) {
        // ✅ Firestore rules requieren auth: request.auth != null
        val uid = currentUidOrNull()
        if (uid == null) {
            throw IllegalStateException("Not authenticated (uid=null). Firestore requires auth.")
        }

        ZodiacSign.entries.forEach { sign ->
            val remote = fetchRemote(dateIso = dateIso, sign = sign, languageCode = languageCode) ?: return@forEach

            val localValue = local.getDaily(dateIso = dateIso, sign = sign, languageCode = languageCode)
            val localUpdated = localValue?.updatedAtEpochMillis ?: 0L

            if (remote.updatedAtEpochMillis > localUpdated) {
                local.saveDaily(remote)
            }
        }


    }

    private suspend fun currentUidOrNull(): String? =
        withTimeoutOrNull(2_000) {
            authRepository.authState
                .filterNotNull()
                .first()
                .uid
        }

    private fun signDocId(sign: ZodiacSign): String = sign.toString().lowercase()

    private fun signDoc(dateIso: String, sign: ZodiacSign) =
        firestore.collection("horoscopeDaily")
            .document(dateIso)
            .collection("signs")
            .document(signDocId(sign))

    private fun languageDoc(dateIso: String, sign: ZodiacSign, languageCode: String) =
        signDoc(dateIso, sign)
            .collection("langs")
            .document(languageCode.lowercase())

    private suspend fun fetchRemote(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? {
        val langSnap = languageDoc(dateIso, sign, languageCode).get()
        if (langSnap.exists) {
            val dto = langSnap.data(HoroscopeDailyRemoteDto.serializer())
            return dto.toDomain(
                sign = sign,
                dateIso = dateIso,
                requestedLanguageCode = languageCode,
                source = HoroscopeRemoteSource.LanguageVariant,
            )
        }

        val legacySnap = signDoc(dateIso, sign).get()
        if (!legacySnap.exists) return null

        // Política explícita: permitimos fallback cross-language al doc legacy para no dejar vacío.
        // Se guarda bajo la clave del idioma solicitado (requestedLanguageCode), aunque el contenido
        // pueda seguir en español hasta que exista backfill multiidioma.
        val dto = legacySnap.data(HoroscopeDailyRemoteDto.serializer())
        return dto.toDomain(
            sign = sign,
            dateIso = dateIso,
            requestedLanguageCode = languageCode,
            source = HoroscopeRemoteSource.LegacyFallback,
        )
    }

    private fun weeklySignDoc(weekKey: String, sign: ZodiacSign) =
        firestore.collection("horoscopeWeekly")
            .document(weekKey)
            .collection("signs")
            .document(signDocId(sign))

    private fun weeklyLanguageDoc(weekKey: String, sign: ZodiacSign, languageCode: String) =
        weeklySignDoc(weekKey, sign).collection("langs").document(languageCode.lowercase())

    private suspend fun fetchWeeklyRemote(weekKey: String, sign: ZodiacSign, languageCode: String): WeeklyHoroscope? {
        val langSnap = weeklyLanguageDoc(weekKey, sign, languageCode).get()
        if (langSnap.exists) {
            val dto = langSnap.data(HoroscopeWeeklyRemoteDto.serializer())
            return dto.toDomain(sign = sign, weekKey = weekKey, requestedLanguageCode = languageCode)
        }
        val canonicalSnap = weeklySignDoc(weekKey, sign).get()
        if (!canonicalSnap.exists) return null
        val dto = canonicalSnap.data(HoroscopeWeeklyRemoteDto.serializer())
        return dto.toDomain(sign = sign, weekKey = weekKey, requestedLanguageCode = languageCode)
    }

    private fun monthlySignDoc(monthKey: String, sign: ZodiacSign) =
        firestore.collection("horoscopeMonthly")
            .document(monthKey)
            .collection("signs")
            .document(signDocId(sign))

    private fun monthlyLanguageDoc(monthKey: String, sign: ZodiacSign, languageCode: String) =
        monthlySignDoc(monthKey, sign).collection("langs").document(languageCode.lowercase())

    private suspend fun fetchMonthlyRemote(monthKey: String, sign: ZodiacSign, languageCode: String): MonthlyHoroscope? {
        val langSnap = monthlyLanguageDoc(monthKey, sign, languageCode).get()
        if (langSnap.exists) {
            val dto = langSnap.data(HoroscopeMonthlyRemoteDto.serializer())
            return dto.toDomain(sign = sign, monthKey = monthKey, requestedLanguageCode = languageCode)
        }
        val canonicalSnap = monthlySignDoc(monthKey, sign).get()
        if (!canonicalSnap.exists) return null
        val dto = canonicalSnap.data(HoroscopeMonthlyRemoteDto.serializer())
        return dto.toDomain(sign = sign, monthKey = monthKey, requestedLanguageCode = languageCode)
    }
}

internal enum class HoroscopeRemoteSource {
    LanguageVariant,
    LegacyFallback,
}

@Serializable
internal data class HoroscopeDailyRemoteDto(
    val text: String,
    val mood: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val shareText: String? = null,
    val languageCode: String? = null,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain(
        sign: ZodiacSign,
        dateIso: String,
        requestedLanguageCode: String,
        source: HoroscopeRemoteSource,
    ): DailyHoroscope {
        val normalizedRequestedLanguage = requestedLanguageCode.lowercase()
        val resolvedLanguageCode = when (source) {
            HoroscopeRemoteSource.LanguageVariant ->
                languageCode?.trim()?.lowercase()?.ifBlank { null } ?: normalizedRequestedLanguage
            HoroscopeRemoteSource.LegacyFallback -> normalizedRequestedLanguage
        }

        return DailyHoroscope(
            sign = sign,
            dateIso = dateIso,
            languageCode = resolvedLanguageCode,
            text = text,
            mood = mood,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            shareText = shareText,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
    }
}
