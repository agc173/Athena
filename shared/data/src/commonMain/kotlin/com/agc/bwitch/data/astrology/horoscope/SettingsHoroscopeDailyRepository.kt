package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.localization.AppLanguage
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class SettingsHoroscopeDailyRepository(
    private val settings: Settings,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    private val cache = MutableStateFlow<Map<String, DailyHoroscope?>>(emptyMap())

    fun observeDaily(dateIso: String, sign: ZodiacSign, languageCode: String): Flow<DailyHoroscope?> {
        val key = key(dateIso, sign, languageCode)
        ensureLoaded(key = key, dateIso = dateIso, sign = sign, languageCode = languageCode)
        return cache.map { it[key] }.distinctUntilChanged()
    }

    suspend fun getDaily(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? {
        val key = key(dateIso, sign, languageCode)
        ensureLoaded(key = key, dateIso = dateIso, sign = sign, languageCode = languageCode)
        return cache.value[key]
    }

    suspend fun saveDaily(value: DailyHoroscope) {
        val key = key(value.dateIso, value.sign, value.languageCode)
        settings.putString(key, json.encodeToString(value))
        cache.value = cache.value + (key to value)
    }

    private fun ensureLoaded(key: String, dateIso: String, sign: ZodiacSign, languageCode: String) {
        if (cache.value.containsKey(key)) return
        val raw = settings.getStringOrNull(key)
        val decoded = raw?.let { decodeFromSettings(it) }
            ?: readLegacyWithoutLanguage(dateIso = dateIso, sign = sign, languageCode = languageCode)
        cache.value = cache.value + (key to decoded)
    }

    private fun decodeFromSettings(raw: String): DailyHoroscope? {
        return runCatching { json.decodeFromString<DailyHoroscope>(raw) }.getOrNull()
            ?: runCatching { json.decodeFromString<LegacyDailyHoroscope>(raw) }.getOrNull()?.toDomain()
    }

    /**
     * Backward compatibility: si existe solo la clave vieja sin idioma, se lee como fallback
     * para evitar mostrar vacío tras actualizar la app.
     */
    private fun readLegacyWithoutLanguage(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? {
        // Política explícita: fallback local de clave legacy solo para `es`.
        // Evitamos etiquetar silenciosamente contenido histórico (normalmente español) como otro idioma.
        if (languageCode.lowercase() != AppLanguage.fallback.code) return null
        val raw = settings.getStringOrNull(legacyKey(dateIso, sign)) ?: return null
        return decodeFromSettings(raw)
    }

    private fun key(dateIso: String, sign: ZodiacSign, languageCode: String): String =
        "horoscope_daily_${dateIso}_${sign.name}_${languageCode.lowercase()}"

    private fun legacyKey(dateIso: String, sign: ZodiacSign): String =
        "horoscope_daily_${dateIso}_${sign.name}"
}

@kotlinx.serialization.Serializable
private data class LegacyDailyHoroscope(
    val sign: ZodiacSign,
    val dateIso: String,
    val text: String,
    val mood: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val shareText: String? = null,
    val updatedAtEpochMillis: Long = 0L,
) {
    fun toDomain(): DailyHoroscope =
        DailyHoroscope(
            sign = sign,
            dateIso = dateIso,
            languageCode = AppLanguage.fallback.code,
            text = text,
            mood = mood,
            luckyNumber = luckyNumber,
            luckyColor = luckyColor,
            shareText = shareText,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}
