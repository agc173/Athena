package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.ZodiacSign
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BasicNatalChartCalculatorTest {
    @Test
    fun calculatesSunAndMoonSignsWithoutAscendantWhenLocationIsNotProvided() {
        val result = BasicNatalChartCalculator().calculate(SunMoonOnlyBirthDateTimeUtc)

        assertWithinTolerance(expected = 112.746, actual = result.sunLongitudeDegrees)
        assertEquals(ZodiacSign.cancer, result.sunSign)
        assertWithinTolerance(expected = 23.257, actual = result.moonLongitudeDegrees)
        assertEquals(ZodiacSign.aries, result.moonSign)
        assertNull(result.ascendantLongitudeDegrees)
        assertNull(result.ascendantSign)
    }

    @Test
    fun calculatesMadridNatalChartWithValidatedAstroSeekCase() {
        val result = BasicNatalChartCalculator().calculate(
            birthDateTimeUtc = BirthDateTimeUtc(
                year = 1994,
                month = 12,
                day = 29,
                hour = 8,
                minute = 0,
                second = 0.0,
            ),
            birthLocation = BirthLocation(latitudeDegrees = 40.4166667, longitudeDegrees = -3.7000000),
        )

        assertValidatedNatalChart(
            result = result,
            expectedSunLongitudeDegrees = degrees(decimalDegrees = 277.0, arcMinutes = 22.0),
            expectedSunSign = ZodiacSign.capricorn,
            expectedMoonLongitudeDegrees = degrees(decimalDegrees = 233.0, arcMinutes = 59.0),
            expectedMoonSign = ZodiacSign.scorpio,
            expectedAscendantLongitudeDegrees = degrees(decimalDegrees = 281.0, arcMinutes = 43.0),
            expectedAscendantSign = ZodiacSign.capricorn,
        )
    }

    @Test
    fun calculatesNewYorkNatalChartWithValidatedAstroSeekCase() {
        val result = BasicNatalChartCalculator().calculate(
            birthDateTimeUtc = BirthDateTimeUtc(
                year = 1980,
                month = 6,
                day = 1,
                hour = 19,
                minute = 10,
                second = 0.0,
            ),
            birthLocation = BirthLocation(latitudeDegrees = 40.7166667, longitudeDegrees = -74.0000000),
        )

        assertValidatedNatalChart(
            result = result,
            expectedSunLongitudeDegrees = degrees(decimalDegrees = 71.0, arcMinutes = 24.0),
            expectedSunSign = ZodiacSign.gemini,
            expectedMoonLongitudeDegrees = degrees(decimalDegrees = 286.0, arcMinutes = 26.0),
            expectedMoonSign = ZodiacSign.capricorn,
            expectedAscendantLongitudeDegrees = degrees(decimalDegrees = 191.0, arcMinutes = 2.0),
            expectedAscendantSign = ZodiacSign.libra,
        )
    }

    @Test
    fun calculatesBeijingNatalChartWithValidatedAstroSeekCase() {
        val result = BasicNatalChartCalculator().calculate(
            birthDateTimeUtc = BirthDateTimeUtc(
                year = 2007,
                month = 10,
                day = 16,
                hour = 6,
                minute = 54,
                second = 0.0,
            ),
            birthLocation = BirthLocation(latitudeDegrees = 39.9000000, longitudeDegrees = 116.4000000),
        )

        assertValidatedNatalChart(
            result = result,
            expectedSunLongitudeDegrees = degrees(decimalDegrees = 202.0, arcMinutes = 32.0),
            expectedSunSign = ZodiacSign.libra,
            expectedMoonLongitudeDegrees = degrees(decimalDegrees = 257.0, arcMinutes = 51.0),
            expectedMoonSign = ZodiacSign.sagittarius,
            expectedAscendantLongitudeDegrees = degrees(decimalDegrees = 318.0, arcMinutes = 45.0),
            expectedAscendantSign = ZodiacSign.aquarius,
        )
    }

    private fun assertValidatedNatalChart(
        result: NatalChartResult,
        expectedSunLongitudeDegrees: Double,
        expectedSunSign: ZodiacSign,
        expectedMoonLongitudeDegrees: Double,
        expectedMoonSign: ZodiacSign,
        expectedAscendantLongitudeDegrees: Double,
        expectedAscendantSign: ZodiacSign,
    ) {
        assertWithinTolerance(expected = expectedSunLongitudeDegrees, actual = result.sunLongitudeDegrees)
        assertEquals(expectedSunSign, result.sunSign)
        assertWithinTolerance(expected = expectedMoonLongitudeDegrees, actual = result.moonLongitudeDegrees)
        assertEquals(expectedMoonSign, result.moonSign)
        assertWithinTolerance(
            expected = expectedAscendantLongitudeDegrees,
            actual = result.ascendantLongitudeDegrees ?: Double.NaN,
        )
        assertEquals(expectedAscendantSign, result.ascendantSign)
    }

    private fun assertWithinTolerance(expected: Double, actual: Double) {
        assertTrue(
            abs(expected - actual) <= LongitudeToleranceDegrees,
            "Expected $actual to be within $LongitudeToleranceDegrees degrees of $expected",
        )
    }

    private fun degrees(decimalDegrees: Double, arcMinutes: Double): Double = decimalDegrees + arcMinutes / 60.0

    private companion object {
        val SunMoonOnlyBirthDateTimeUtc = BirthDateTimeUtc(
            year = 1990,
            month = 7,
            day = 15,
            hour = 12,
            minute = 0,
            second = 0.0,
        )
        const val LongitudeToleranceDegrees = 0.25
    }
}
