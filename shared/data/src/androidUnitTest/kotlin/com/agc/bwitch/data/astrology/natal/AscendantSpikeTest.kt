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

// Spike-only test: validates the direct Ascendant formula without changing production behavior.
class AscendantSpikeTest {
    @Test
    fun validatesRawAndCorrectedAscendantAgainstAstroSeekReferences() {
        val calculator = BasicNatalChartCalculator()
        val measurements = referenceCases.map { case ->
            val natalChart = calculator.calculate(case.birthDateTimeUtc)
            val rawAscendant = calculateRawAscendantLongitudeDegrees(
                birthDateTimeUtc = case.birthDateTimeUtc,
                latitudeDegrees = case.latitudeDegrees,
                longitudeDegrees = case.longitudeDegrees,
            )
            val correctedAscendant = normalizeDegrees(rawAscendant + OppositeHorizonDegrees)

            Measurement(
                case = case,
                sunLongitudeDegrees = natalChart.sunLongitudeDegrees,
                sunSign = natalChart.sunSign,
                moonLongitudeDegrees = natalChart.moonLongitudeDegrees,
                moonSign = natalChart.moonSign,
                rawAscendantLongitudeDegrees = rawAscendant,
                rawAscendantSign = longitudeToZodiacSign(rawAscendant),
                correctedAscendantLongitudeDegrees = correctedAscendant,
                correctedAscendantSign = longitudeToZodiacSign(correctedAscendant),
            )
        }

        measurements.forEach { measurement ->
            printMeasurement(measurement)

            assertWithinTolerance(
                label = "${measurement.case.name} Sun",
                expected = measurement.case.expectedSunLongitudeDegrees,
                actual = measurement.sunLongitudeDegrees,
            )
            assertEquals(measurement.case.expectedSunSign, measurement.sunSign, "${measurement.case.name} Sun sign")

            assertWithinTolerance(
                label = "${measurement.case.name} Moon",
                expected = measurement.case.expectedMoonLongitudeDegrees,
                actual = measurement.moonLongitudeDegrees,
            )
            assertEquals(measurement.case.expectedMoonSign, measurement.moonSign, "${measurement.case.name} Moon sign")

            assertWithinTolerance(
                label = "${measurement.case.name} corrected Ascendant",
                expected = measurement.case.expectedAscendantLongitudeDegrees,
                actual = measurement.correctedAscendantLongitudeDegrees,
            )
            assertEquals(
                measurement.case.expectedAscendantSign,
                measurement.correctedAscendantSign,
                "${measurement.case.name} corrected Ascendant sign",
            )
        }
    }

    private fun calculateRawAscendantLongitudeDegrees(
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
        val localSiderealDegrees = normalizeDegrees((siderealTime(time) + longitudeDegrees / HoursToDegrees) * HoursToDegrees)
        val theta = localSiderealDegrees.toRadians()
        val latitude = latitudeDegrees.toRadians()
        val obliquity = MeanObliquityDegrees.toRadians()

        val longitude = atan2(
            y = -cos(theta),
            x = sin(theta) * cos(obliquity) + tan(latitude) * sin(obliquity),
        ).toDegrees()

        return normalizeDegrees(longitude)
    }

    private fun printMeasurement(measurement: Measurement) {
        val case = measurement.case
        println("--------------------------------")
        println(case.name)
        println("Sun")
        println("actual longitude: ${measurement.sunLongitudeDegrees.formatDegrees()}")
        println("expected longitude: ${case.expectedSunLongitudeDegrees.formatDegrees()}")
        println("delta: ${degreesDelta(measurement.sunLongitudeDegrees, case.expectedSunLongitudeDegrees).formatDegrees()}")
        println("actual sign: ${measurement.sunSign.label}")
        println("expected sign: ${case.expectedSunSign.label}")
        println("Moon")
        println("actual longitude: ${measurement.moonLongitudeDegrees.formatDegrees()}")
        println("expected longitude: ${case.expectedMoonLongitudeDegrees.formatDegrees()}")
        println("delta: ${degreesDelta(measurement.moonLongitudeDegrees, case.expectedMoonLongitudeDegrees).formatDegrees()}")
        println("actual sign: ${measurement.moonSign.label}")
        println("expected sign: ${case.expectedMoonSign.label}")
        println("Ascendant RAW")
        println("longitude: ${measurement.rawAscendantLongitudeDegrees.formatDegrees()}")
        println("sign: ${measurement.rawAscendantSign.label}")
        println("delta vs Astro-Seek: ${degreesDelta(measurement.rawAscendantLongitudeDegrees, case.expectedAscendantLongitudeDegrees).formatDegrees()}")
        println("Ascendant CORRECTED")
        println("longitude: ${measurement.correctedAscendantLongitudeDegrees.formatDegrees()}")
        println("sign: ${measurement.correctedAscendantSign.label}")
        println("delta vs Astro-Seek: ${degreesDelta(measurement.correctedAscendantLongitudeDegrees, case.expectedAscendantLongitudeDegrees).formatDegrees()}")
        println("--------------------------------")
    }

    private fun assertWithinTolerance(label: String, expected: Double, actual: Double) {
        val delta = degreesDelta(actual = actual, expected = expected)
        assertTrue(
            actual = delta <= AstroSeekToleranceDegrees,
            message = "$label expected ${expected.formatDegrees()}, got ${actual.formatDegrees()}, delta ${delta.formatDegrees()}",
        )
    }

    private data class ReferenceCase(
        val name: String,
        val birthDateTimeUtc: BirthDateTimeUtc,
        val latitudeDegrees: Double,
        val longitudeDegrees: Double,
        val expectedSunLongitudeDegrees: Double,
        val expectedSunSign: ZodiacSign,
        val expectedMoonLongitudeDegrees: Double,
        val expectedMoonSign: ZodiacSign,
        val expectedAscendantLongitudeDegrees: Double,
        val expectedAscendantSign: ZodiacSign,
    )

    private data class Measurement(
        val case: ReferenceCase,
        val sunLongitudeDegrees: Double,
        val sunSign: ZodiacSign,
        val moonLongitudeDegrees: Double,
        val moonSign: ZodiacSign,
        val rawAscendantLongitudeDegrees: Double,
        val rawAscendantSign: ZodiacSign,
        val correctedAscendantLongitudeDegrees: Double,
        val correctedAscendantSign: ZodiacSign,
    )

    private companion object {
        const val AstroSeekToleranceDegrees = 0.25
        const val HoursToDegrees = 15.0
        const val MeanObliquityDegrees = 23.4392911
        const val OppositeHorizonDegrees = 180.0

        val referenceCases = listOf(
            ReferenceCase(
                name = "CASE 1 — Madrid",
                birthDateTimeUtc = BirthDateTimeUtc(1994, 12, 29, 8, 0, 0.0),
                latitudeDegrees = 40.4166667,
                longitudeDegrees = -3.7000000,
                expectedSunLongitudeDegrees = degreesAndMinutesToDecimal(277, 22),
                expectedSunSign = ZodiacSign.capricorn,
                expectedMoonLongitudeDegrees = degreesAndMinutesToDecimal(233, 59),
                expectedMoonSign = ZodiacSign.scorpio,
                expectedAscendantLongitudeDegrees = degreesAndMinutesToDecimal(281, 43),
                expectedAscendantSign = ZodiacSign.capricorn,
            ),
            ReferenceCase(
                name = "CASE 2 — New York",
                birthDateTimeUtc = BirthDateTimeUtc(1980, 6, 1, 19, 10, 0.0),
                latitudeDegrees = 40.7166667,
                longitudeDegrees = -74.0000000,
                expectedSunLongitudeDegrees = degreesAndMinutesToDecimal(71, 24),
                expectedSunSign = ZodiacSign.gemini,
                expectedMoonLongitudeDegrees = degreesAndMinutesToDecimal(286, 26),
                expectedMoonSign = ZodiacSign.capricorn,
                expectedAscendantLongitudeDegrees = degreesAndMinutesToDecimal(191, 2),
                expectedAscendantSign = ZodiacSign.libra,
            ),
            ReferenceCase(
                name = "CASE 3 — Beijing",
                birthDateTimeUtc = BirthDateTimeUtc(2007, 10, 16, 6, 54, 0.0),
                latitudeDegrees = 39.9000000,
                longitudeDegrees = 116.4000000,
                expectedSunLongitudeDegrees = degreesAndMinutesToDecimal(202, 32),
                expectedSunSign = ZodiacSign.libra,
                expectedMoonLongitudeDegrees = degreesAndMinutesToDecimal(257, 51),
                expectedMoonSign = ZodiacSign.sagittarius,
                expectedAscendantLongitudeDegrees = degreesAndMinutesToDecimal(318, 45),
                expectedAscendantSign = ZodiacSign.aquarius,
            ),
        )
    }
}

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI

private fun normalizeDegrees(degrees: Double): Double = ((degrees % 360.0) + 360.0) % 360.0

private fun degreesDelta(actual: Double, expected: Double): Double {
    val directDelta = abs(normalizeDegrees(actual) - normalizeDegrees(expected))
    return minOf(directDelta, 360.0 - directDelta)
}

private fun degreesAndMinutesToDecimal(degrees: Int, minutes: Int): Double = degrees + minutes / 60.0

private fun Double.formatDegrees(): String = "%.4f°".format(this)
