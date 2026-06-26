package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.ZodiacSign
import com.agc.bwitch.domain.astrology.natal.longitudeToZodiacSign
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.siderealTime
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// TODO(feature/basic-natal-chart): spike-only ascendant validation
class AscendantSpikeTest {
    @Test
    fun calculatesAscendantRegressionCasesLocally() {
        val cases = listOf(
            Case(
                birthDateTimeUtc = BirthDateTimeUtc(2000, 1, 1, 12, 0, 0.0),
                latitudeDegrees = 51.4769,
                longitudeDegrees = 0.0,
                expectedLongitudeDegrees = 204.3,
                expectedSign = ZodiacSign.libra,
            ),
            Case(
                birthDateTimeUtc = BirthDateTimeUtc(1990, 6, 15, 18, 30, 0.0),
                latitudeDegrees = 40.7128,
                longitudeDegrees = -74.0060,
                expectedLongitudeDegrees = 13.7,
                expectedSign = ZodiacSign.aries,
            ),
            Case(
                birthDateTimeUtc = BirthDateTimeUtc(2024, 3, 20, 3, 6, 0.0),
                latitudeDegrees = -33.8688,
                longitudeDegrees = 151.2093,
                expectedLongitudeDegrees = 269.0,
                expectedSign = ZodiacSign.sagittarius,
            ),
        )

        cases.forEach { case ->
            val longitude = calculateAscendantLongitudeDegrees(
                birthDateTimeUtc = case.birthDateTimeUtc,
                latitudeDegrees = case.latitudeDegrees,
                longitudeDegrees = case.longitudeDegrees,
            )

            assertTrue(
                actual = abs(longitude - case.expectedLongitudeDegrees) < 1.0,
                message = "Expected ${case.expectedLongitudeDegrees}°, got $longitude° for $case",
            )
            assertEquals(case.expectedSign, longitudeToZodiacSign(longitude))
        }
    }

    private fun calculateAscendantLongitudeDegrees(
        birthDateTimeUtc: BirthDateTimeUtc,
        latitudeDegrees: Double,
        longitudeDegrees: Double,
    ): Double {
        val time = Time(
            birthDateTimeUtc.year,
            birthDateTimeUtc.month,
            birthDateTimeUtc.day,
            birthDateTimeUtc.hour,
            birthDateTimeUtc.minute,
            birthDateTimeUtc.second,
        )
        val localSiderealDegrees = normalizeDegrees((siderealTime(time) + longitudeDegrees / 15.0) * 15.0)
        val theta = localSiderealDegrees.toRadians()
        val latitude = latitudeDegrees.toRadians()
        val obliquity = MeanObliquityDegrees.toRadians()

        val longitude = atan2(
            y = -cos(theta),
            x = sin(theta) * cos(obliquity) + tan(latitude) * sin(obliquity),
        ).toDegrees()

        return normalizeDegrees(longitude)
    }

    private data class Case(
        val birthDateTimeUtc: BirthDateTimeUtc,
        val latitudeDegrees: Double,
        val longitudeDegrees: Double,
        val expectedLongitudeDegrees: Double,
        val expectedSign: ZodiacSign,
    )

    private companion object {
        const val MeanObliquityDegrees = 23.4392911
    }
}

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI

private fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0
