package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.ZodiacSign
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BasicNatalChartCalculatorTest {
    @Test
    fun calculatesSunAndMoonSignsForSpikeValidationDate() {
        val result = BasicNatalChartCalculator().calculate(
            BirthDateTimeUtc(
                year = 1990,
                month = 7,
                day = 15,
                hour = 12,
                minute = 0,
                second = 0.0,
            )
        )

        assertWithinTolerance(expected = 112.746, actual = result.sunLongitudeDegrees)
        assertEquals(ZodiacSign.cancer, result.sunSign)
        assertWithinTolerance(expected = 23.257, actual = result.moonLongitudeDegrees)
        assertEquals(ZodiacSign.aries, result.moonSign)
    }

    private fun assertWithinTolerance(expected: Double, actual: Double) {
        assertTrue(
            abs(expected - actual) < 0.01,
            "Expected $actual to be within 0.01 degrees of $expected",
        )
    }
}
