package com.agc.bwitch

import com.agc.bwitch.ui.astrology.birthplace.normalizeBirthplaceSearchText
import kotlin.test.Test
import kotlin.test.assertEquals

class BirthplaceSearchTextNormalizerTest {

    @Test
    fun removesCommonLatinDiacriticsAndLowercasesText() {
        assertEquals("malaga", normalizeBirthplaceSearchText("Málaga"))
        assertEquals("sao paulo", normalizeBirthplaceSearchText("São Paulo"))
        assertEquals("bogota", normalizeBirthplaceSearchText("Bogotá"))
        assertEquals("munchen", normalizeBirthplaceSearchText("München"))
    }

    @Test
    fun supportsMultiCharacterFolds() {
        assertEquals("strasse", normalizeBirthplaceSearchText("Straße"))
    }
}
