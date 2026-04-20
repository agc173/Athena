package com.agc.bwitch.data.astrology.birthchart

import kotlin.test.Test
import kotlin.test.assertEquals

class BirthEssenceRemoteDtoTest {

    @Test
    fun toDomain_withoutLanguageCode_fallsBackToSpanish_forLegacyDocs() {
        val dto = BirthEssenceRemoteDto(
            sunSign = "aries",
            moonSign = "leo",
            risingSign = "sagittarius",
            interpretation = "Hola",
            languageCode = null,
            archetype = "MISTICA",
            savedAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
        )

        val result = dto.toDomain()

        assertEquals("es", result.languageCode)
    }

    @Test
    fun toDomain_withRegionalLanguageCode_keepsBaseLanguage() {
        val dto = BirthEssenceRemoteDto(
            sunSign = "aries",
            moonSign = "leo",
            risingSign = "sagittarius",
            interpretation = "Hello",
            languageCode = "EN-US",
            archetype = "MISTICA",
            savedAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L,
        )

        val result = dto.toDomain()

        assertEquals("en", result.languageCode)
    }
}
