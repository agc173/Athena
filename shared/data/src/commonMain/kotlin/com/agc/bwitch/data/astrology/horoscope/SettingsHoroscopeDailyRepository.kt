package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
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

    fun observeDaily(dateIso: String, sign: ZodiacSign): Flow<DailyHoroscope?> {
        val key = key(dateIso, sign)
        ensureLoaded(key)
        return cache.map { it[key] }.distinctUntilChanged()
    }

    suspend fun getDaily(dateIso: String, sign: ZodiacSign): DailyHoroscope? {
        val key = key(dateIso, sign)
        ensureLoaded(key)
        return cache.value[key]
    }

    suspend fun saveDaily(value: DailyHoroscope) {
        val key = key(value.dateIso, value.sign)
        settings.putString(key, json.encodeToString(value))
        cache.value = cache.value + (key to value)
    }

    private fun ensureLoaded(key: String) {
        if (cache.value.containsKey(key)) return
        val raw = settings.getStringOrNull(key)
        val decoded = raw?.let { runCatching { json.decodeFromString<DailyHoroscope>(it) }.getOrNull() }
        cache.value = cache.value + (key to decoded)
    }

    private fun key(dateIso: String, sign: ZodiacSign): String =
        "horoscope_daily_${dateIso}_${sign.name}"
}