package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsHoroscopeDailyRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun getDaily_usesLanguageSpecificCacheKey() = runBlocking {
        val settings = MapSettings()
        val repository = SettingsHoroscopeDailyRepository(settings, json)

        repository.saveDaily(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-04-09",
                languageCode = "en",
                text = "English text",
                mood = "Confident",
                luckyNumber = 7,
                luckyColor = "Blue",
            )
        )

        val fromEnglishKey = repository.getDaily("2026-04-09", ZodiacSign.aries, "en")
        val fromSpanishKey = repository.getDaily("2026-04-09", ZodiacSign.aries, "es")

        assertEquals("en", fromEnglishKey?.languageCode)
        assertNull(fromSpanishKey)
    }

    @Test
    fun getDaily_readsLegacyKey_whenRequestedLanguageIsSpanish() = runBlocking {
        val settings = MapSettings()
        val repository = SettingsHoroscopeDailyRepository(settings, json)

        settings.putString(
            "horoscope_daily_2026-04-09_aries",
            json.encodeToString(
                LegacySettingsHoroscopeDaily(
                    sign = ZodiacSign.aries,
                    dateIso = "2026-04-09",
                    text = "Texto legacy",
                    mood = "Calma",
                    luckyNumber = 3,
                    luckyColor = "Rojo",
                )
            )
        )

        val result = repository.getDaily("2026-04-09", ZodiacSign.aries, "es")

        assertEquals("es", result?.languageCode)
        assertEquals("Texto legacy", result?.text)
    }

    @Test
    fun getDaily_doesNotReadLegacyKey_whenRequestedLanguageIsNotSpanish() = runBlocking {
        val settings = MapSettings()
        val repository = SettingsHoroscopeDailyRepository(settings, json)

        settings.putString(
            "horoscope_daily_2026-04-09_aries",
            json.encodeToString(
                LegacySettingsHoroscopeDaily(
                    sign = ZodiacSign.aries,
                    dateIso = "2026-04-09",
                    text = "Texto legacy",
                    mood = "Calma",
                    luckyNumber = 3,
                    luckyColor = "Rojo",
                )
            )
        )

        val result = repository.getDaily("2026-04-09", ZodiacSign.aries, "en")
        assertNull(result)
    }
}

@kotlinx.serialization.Serializable
private data class LegacySettingsHoroscopeDaily(
    val sign: ZodiacSign,
    val dateIso: String,
    val text: String,
    val mood: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val shareText: String? = null,
    val updatedAtEpochMillis: Long = 0L,
)
