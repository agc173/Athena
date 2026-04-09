package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.test.Test
import kotlin.test.assertEquals

class HoroscopeDailyRemoteDtoTest {

    private val dto = HoroscopeDailyRemoteDto(
        text = "Texto",
        mood = "Bien",
        luckyNumber = 7,
        luckyColor = "Azul",
        shareText = "Comparte",
        updatedAtEpochMillis = 123L,
    )

    @Test
    fun toDomain_requestSpanish_withLanguageVariant_keepsSpanishLanguageCode() {
        val result = dto.toDomain(
            sign = ZodiacSign.aries,
            dateIso = "2026-04-09",
            requestedLanguageCode = "es",
            source = HoroscopeRemoteSource.LanguageVariant,
        )

        assertEquals("es", result.languageCode)
    }

    @Test
    fun toDomain_requestEnglish_withEnglishLanguageVariant_keepsEnglishLanguageCode() {
        val withLanguage = dto.copy(languageCode = "en")

        val result = withLanguage.toDomain(
            sign = ZodiacSign.aries,
            dateIso = "2026-04-09",
            requestedLanguageCode = "en",
            source = HoroscopeRemoteSource.LanguageVariant,
        )

        assertEquals("en", result.languageCode)
    }

    @Test
    fun toDomain_requestEnglish_withLegacyFallback_usesRequestedLanguageCodeForCacheKeyConsistency() {
        val result = dto.toDomain(
            sign = ZodiacSign.aries,
            dateIso = "2026-04-09",
            requestedLanguageCode = "en",
            source = HoroscopeRemoteSource.LegacyFallback,
        )

        assertEquals("en", result.languageCode)
    }
}
