package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.longitudeToZodiacSign
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.sunPosition

class BasicNatalChartCalculator {
    fun calculate(birthDateTimeUtc: BirthDateTimeUtc): NatalChartResult {
        val time = Time(
            birthDateTimeUtc.year,
            birthDateTimeUtc.month,
            birthDateTimeUtc.day,
            birthDateTimeUtc.hour,
            birthDateTimeUtc.minute,
            birthDateTimeUtc.second,
        )
        val sunLongitude = sunPosition(time).elon
        val moonLongitude = eclipticGeoMoon(time).lon

        return NatalChartResult(
            sunLongitudeDegrees = sunLongitude,
            sunSign = longitudeToZodiacSign(sunLongitude),
            moonLongitudeDegrees = moonLongitude,
            moonSign = longitudeToZodiacSign(moonLongitude),
        )
    }
}
