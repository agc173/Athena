package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.longitudeToZodiacSign
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Common natal engine for the scoped Sol/Luna/Ascendente calculation.
 *
 * This implementation is the production runtime on Android and iOS. The Android-only
 * Astronomy Engine implementation is kept out of production and used only by the precision audit
 * as an oracle for future regression detection.
 */
class BasicNatalChartCalculator {
    fun calculate(
        birthDateTimeUtc: BirthDateTimeUtc,
        birthLocation: BirthLocation? = null,
    ): NatalChartResult {
        val time = NatalTime.fromUtc(birthDateTimeUtc)
        val sunLongitude = sunLongitudeDegrees(time)
        val moonLongitude = moonLongitudeDegrees(time)
        val ascendantLongitude = birthLocation?.let { calculateAscendantLongitudeDegrees(time, it) }

        return NatalChartResult(
            sunLongitudeDegrees = sunLongitude,
            sunSign = longitudeToZodiacSign(sunLongitude),
            moonLongitudeDegrees = moonLongitude,
            moonSign = longitudeToZodiacSign(moonLongitude),
            ascendantLongitudeDegrees = ascendantLongitude,
            ascendantSign = ascendantLongitude?.let(::longitudeToZodiacSign),
        )
    }

    fun calculate(birthDateTimeUtc: BirthDateTimeUtc): NatalChartResult = calculate(birthDateTimeUtc, null)

    private fun calculateAscendantLongitudeDegrees(time: NatalTime, birthLocation: BirthLocation): Double {
        val localSiderealDegrees = normalizeDegrees(
            degrees = (siderealTimeHours(time) + birthLocation.longitudeDegrees / HoursToDegrees) * HoursToDegrees,
        )
        val theta = localSiderealDegrees.toRadians()
        val latitude = birthLocation.latitudeDegrees.toRadians()
        val obliquity = MeanObliquityDegrees.toRadians()

        val rawLongitude = atan2(
            y = -cos(theta),
            x = sin(theta) * cos(obliquity) + tan(latitude) * sin(obliquity),
        ).toDegrees()

        return normalizeDegrees(rawLongitude + OppositePointDegrees)
    }

    private companion object {
        const val MeanObliquityDegrees = 23.4392911
        const val OppositePointDegrees = 180.0
        const val HoursToDegrees = 15.0
    }
}

private data class NatalTime(val julianDayUt: Double, val julianDayTt: Double) {
    val daysSinceJ2000Ut: Double = julianDayUt - J2000_JD
    val daysSinceJ2000Tt: Double = julianDayTt - J2000_JD
    val centuriesSinceJ2000Tt: Double = daysSinceJ2000Tt / 36525.0

    companion object {
        fun fromUtc(value: BirthDateTimeUtc): NatalTime {
            val jdUt = julianDay(value.year, value.month, value.day, value.hour, value.minute, value.second)
            val deltaTSeconds = estimateDeltaTSeconds(value.year, value.month)
            return NatalTime(julianDayUt = jdUt, julianDayTt = jdUt + deltaTSeconds / SECONDS_PER_DAY)
        }
    }
}

private fun siderealTimeHours(time: NatalTime): Double {
    val t = (time.julianDayUt - J2000_JD) / 36525.0
    val degrees = 280.46061837 + 360.98564736629 * time.daysSinceJ2000Ut +
        0.000387933 * t * t - t * t * t / 38710000.0
    return normalizeDegrees(degrees) / 15.0
}

private fun sunLongitudeDegrees(time: NatalTime): Double {
    val t = time.centuriesSinceJ2000Tt
    val meanLongitude = normalizeDegrees(280.46646 + 36000.76983 * t + 0.0003032 * t * t)
    val meanAnomaly = normalizeDegrees(357.52911 + 35999.05029 * t - 0.0001537 * t * t).toRadians()
    val equationOfCenter = (1.914602 - 0.004817 * t - 0.000014 * t * t) * sin(meanAnomaly) +
        (0.019993 - 0.000101 * t) * sin(2.0 * meanAnomaly) +
        0.000289 * sin(3.0 * meanAnomaly)
    val omega = (125.04 - 1934.136 * t).toRadians()
    return normalizeDegrees(meanLongitude + equationOfCenter - 0.00569 - 0.00478 * sin(omega))
}

private fun moonLongitudeDegrees(time: NatalTime): Double {
    val d = time.daysSinceJ2000Tt
    val lPrime = normalizeDegrees(218.3164477 + 13.17639648 * d)
    val moonMeanAnomaly = normalizeDegrees(134.9633964 + 13.06499295 * d).toRadians()
    val sunMeanAnomaly = normalizeDegrees(357.5291092 + 0.98560028 * d).toRadians()
    val elongation = normalizeDegrees(297.8501921 + 12.19074912 * d).toRadians()
    val argumentLatitude = normalizeDegrees(93.2720950 + 13.22935024 * d).toRadians()

    return normalizeDegrees(
        lPrime +
            6.289 * sin(moonMeanAnomaly) +
            1.274 * sin(2.0 * elongation - moonMeanAnomaly) +
            0.658 * sin(2.0 * elongation) +
            0.214 * sin(2.0 * moonMeanAnomaly) -
            0.186 * sin(sunMeanAnomaly) -
            0.114 * sin(2.0 * argumentLatitude) +
            0.059 * sin(2.0 * elongation - 2.0 * moonMeanAnomaly) +
            0.057 * sin(2.0 * elongation - sunMeanAnomaly - moonMeanAnomaly) +
            0.053 * sin(2.0 * elongation + moonMeanAnomaly) +
            0.046 * sin(2.0 * elongation - sunMeanAnomaly) +
            0.041 * sin(sunMeanAnomaly - moonMeanAnomaly),
    )
}

private fun julianDay(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Double): Double {
    val adjustedYear = if (month <= 2) year - 1 else year
    val adjustedMonth = if (month <= 2) month + 12 else month
    val a = floor(adjustedYear / 100.0)
    val b = 2.0 - a + floor(a / 4.0)
    val dayFraction = (hour + (minute + second / 60.0) / 60.0) / 24.0
    return floor(365.25 * (adjustedYear + 4716)) + floor(30.6001 * (adjustedMonth + 1)) + day + dayFraction + b - 1524.5
}

private fun estimateDeltaTSeconds(year: Int, month: Int): Double {
    val y = year + (month - 0.5) / 12.0
    val t = y - 2000.0
    return 62.92 + 0.32217 * t + 0.005589 * t * t
}

private fun Double.toRadians(): Double = this * PI / 180.0
private fun Double.toDegrees(): Double = this * 180.0 / PI
private fun normalizeDegrees(degrees: Double): Double = degrees - floor(degrees / FULL_CIRCLE_DEGREES) * FULL_CIRCLE_DEGREES
private const val FULL_CIRCLE_DEGREES = 360.0
private const val J2000_JD = 2451545.0
private const val SECONDS_PER_DAY = 86400.0
