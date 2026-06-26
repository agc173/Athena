package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.longitudeToZodiacSign
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.siderealTime
import io.github.cosinekitty.astronomy.sunPosition
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

class BasicNatalChartCalculator {
    fun calculate(
        birthDateTimeUtc: BirthDateTimeUtc,
        birthLocation: BirthLocation? = null,
    ): NatalChartResult {
        val time = birthDateTimeUtc.toAstronomyTime()
        val sunLongitude = sunPosition(time).elon
        val moonLongitude = eclipticGeoMoon(time).lon
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

    private fun calculateAscendantLongitudeDegrees(
        time: Time,
        birthLocation: BirthLocation,
    ): Double {
        val localSiderealDegrees = normalizeDegrees(
            degrees = (siderealTime(time) + birthLocation.longitudeDegrees / HoursToDegrees) * HoursToDegrees,
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

    private fun BirthDateTimeUtc.toAstronomyTime(): Time = Time(
        year,
        month,
        day,
        hour,
        minute,
        second,
    )

    private companion object {
        const val MeanObliquityDegrees = 23.4392911
        const val OppositePointDegrees = 180.0
        const val HoursToDegrees = 15.0
    }
}

private fun Double.toRadians(): Double = this * PI / 180.0

private fun Double.toDegrees(): Double = this * 180.0 / PI

private fun normalizeDegrees(degrees: Double): Double =
    degrees - (floor(degrees / FULL_CIRCLE_DEGREES) * FULL_CIRCLE_DEGREES)

private const val FULL_CIRCLE_DEGREES = 360.0
