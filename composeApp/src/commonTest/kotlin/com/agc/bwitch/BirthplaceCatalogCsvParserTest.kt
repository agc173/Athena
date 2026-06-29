package com.agc.bwitch

import com.agc.bwitch.ui.astrology.birthplace.BirthplaceCatalogCsvParser
import kotlin.test.Test
import kotlin.test.assertEquals

class BirthplaceCatalogCsvParserTest {

    @Test
    fun ignoresHeaderAndParsesValidLine() {
        val presets = BirthplaceCatalogCsvParser.parse(
            """
            geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode
            3117735,Madrid,Spain,ES,40.4165,-3.7026,Europe/Madrid,3255944,PPLC
            """.trimIndent()
        )

        assertEquals(1, presets.size)
        assertEquals("madrid-es-3117735", presets.single().id)
    }

    @Test
    fun ignoresEmptyAndCorruptLines() {
        val presets = BirthplaceCatalogCsvParser.parse(
            """
            geonameId,cityName,countryName,countryCode,latitudeDegrees,longitudeDegrees,timezoneId,population,featureCode

            corrupt-line
            123,MissingLatitude,Spain,ES,,2.0,Europe/Madrid,1000,PPL
            3117735,Madrid,Spain,ES,40.4165,-3.7026,Europe/Madrid,3255944,PPLC
            """.trimIndent()
        )

        assertEquals(1, presets.size)
        assertEquals("Madrid", presets.single().cityName)
    }

    @Test
    fun preservesCoreBirthplaceFields() {
        val preset = BirthplaceCatalogCsvParser.parse(
            "3117735,Madrid,Spain,ES,40.4165,-3.7026,Europe/Madrid,3255944,PPLC"
        ).single()

        assertEquals("Madrid", preset.cityName)
        assertEquals("ES", preset.countryCode)
        assertEquals(40.4165, preset.latitudeDegrees)
        assertEquals(-3.7026, preset.longitudeDegrees)
        assertEquals("Europe/Madrid", preset.timezoneId)
    }

    @Test
    fun supportsQuotedCsvFields() {
        val preset = BirthplaceCatalogCsvParser.parse(
            "5128581,New York,\"United States\",US,40.7128,-74.0060,America/New_York,8804190,PPL"
        ).single()

        assertEquals("New York", preset.cityName)
        assertEquals("United States", preset.countryName)
    }
}
