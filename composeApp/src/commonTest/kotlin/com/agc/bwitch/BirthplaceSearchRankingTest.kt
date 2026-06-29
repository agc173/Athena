package com.agc.bwitch

import com.agc.bwitch.domain.astrology.natal.BirthplacePreset
import com.agc.bwitch.ui.astrology.birthplace.rankBirthplaceMatches
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BirthplaceSearchRankingTest {

    @Test
    fun ranksMadridSpainBeforeOtherMadridWhenFixtureOrdersPrimaryCityLast() {
        val results = rankBirthplaceMatches(
            query = "madrid",
            presets = listOf(
                birthplace("madrid-co", "Madrid", "Colombia", "CO", "America/Bogota"),
                birthplace("madrid-us", "Madrid", "United States", "US", "America/Chicago"),
                birthplace("madrid-es", "Madrid", "Spain", "ES", "Europe/Madrid"),
            ),
        )

        assertEquals("madrid-es", results.first().id)
    }

    @Test
    fun ranksTokyoJapanAtTopWhenItAppearsAfterOtherTokyos() {
        val results = rankBirthplaceMatches(
            query = "tokyo",
            presets = listOf(
                birthplace("tokyo-us", "Tokyo", "United States", "US", "America/New_York"),
                birthplace("west-tokyo", "West Tokyo", "Japan", "JP", "Asia/Tokyo"),
                birthplace("tokyo-jp", "Tokyo", "Japan", "JP", "Asia/Tokyo"),
            ),
        )

        assertEquals("tokyo-jp", results.first().id)
    }

    @Test
    fun ranksMoscowRussiaAtTopWhenItAppearsAfterOtherMoscows() {
        val results = rankBirthplaceMatches(
            query = "moscow",
            presets = listOf(
                birthplace("moscow-us", "Moscow", "United States", "US", "America/New_York"),
                birthplace("moscow-mills", "Moscow Mills", "United States", "US", "America/Chicago"),
                birthplace("moscow-ru", "Moscow", "Russia", "RU", "Europe/Moscow"),
                birthplace("new-moscow", "New Moscow", "United States", "US", "America/New_York"),
            ),
        )

        assertEquals("moscow-ru", results.first().id)
    }

    @Test
    fun ranksBuenosAiresArgentinaAtTopWhenItAppearsAfterOtherBuenosAires() {
        val results = rankBirthplaceMatches(
            query = "buenos aires",
            presets = listOf(
                birthplace("buenos-aires-cr", "Buenos Aires", "Costa Rica", "CR", "America/Costa_Rica"),
                birthplace("aires", "Aires", "Argentina", "AR", "America/Argentina/Buenos_Aires"),
                birthplace("buenos-aires-ar", "Buenos Aires", "Argentina", "AR", "America/Argentina/Buenos_Aires"),
            ),
        )

        assertEquals("buenos-aires-ar", results.first().id)
    }

    @Test
    fun countryMatchesDoNotDisplaceExactCityMatches() {
        val results = rankBirthplaceMatches(
            query = "spain",
            presets = listOf(
                birthplace("madrid-es", "Madrid", "Spain", "ES", "Europe/Madrid"),
                birthplace("spain-us", "Spain", "United States", "US", "America/New_York"),
                birthplace("barcelona-es", "Barcelona", "Spain", "ES", "Europe/Madrid"),
            ),
        )

        assertEquals("spain-us", results.first().id)
        assertTrue(results.drop(1).all { it.countryName == "Spain" })
    }

    @Test
    fun doesNotMatchBirthplacesByTimezoneOnly() {
        val presets = listOf(
            birthplace("madrid-es", "Madrid", "Spain", "ES", "Europe/Madrid"),
            birthplace("barcelona-es", "Barcelona", "Spain", "ES", "Europe/Madrid"),
            birthplace("moscow-ru", "Moscow", "Russia", "RU", "Europe/Moscow"),
            birthplace("saint-petersburg-ru", "Saint Petersburg", "Russia", "RU", "Europe/Moscow"),
            birthplace("tokyo-jp", "Tokyo", "Japan", "JP", "Asia/Tokyo"),
            birthplace("osaka-jp", "Osaka", "Japan", "JP", "Asia/Tokyo"),
            birthplace("bogota-co", "Bogota", "Colombia", "CO", "America/Bogota"),
            birthplace("cali-co", "Cali", "Colombia", "CO", "America/Bogota"),
        )

        assertEquals(listOf("madrid-es"), rankBirthplaceMatches("madrid", presets).map { it.id })
        assertEquals(listOf("moscow-ru"), rankBirthplaceMatches("moscow", presets).map { it.id })
        assertEquals(listOf("tokyo-jp"), rankBirthplaceMatches("tokyo", presets).map { it.id })
        assertEquals(listOf("bogota-co"), rankBirthplaceMatches("bogota", presets).map { it.id })
    }

    @Test
    fun emptyQueryKeepsOriginalOrderAndLimit() {
        val presets = (1..25).map { index ->
            birthplace("city-$index", "City $index", "Country", "CO", "Etc/UTC")
        }

        val results = rankBirthplaceMatches(query = " ", presets = presets)

        assertEquals((1..20).map { "city-$it" }, results.map { it.id })
    }

    private fun birthplace(
        id: String,
        cityName: String,
        countryName: String,
        countryCode: String,
        timezoneId: String,
    ): BirthplacePreset = BirthplacePreset(
        id = id,
        cityName = cityName,
        countryName = countryName,
        latitudeDegrees = 0.0,
        longitudeDegrees = 0.0,
        timezoneId = timezoneId,
        countryCode = countryCode,
    )
}
