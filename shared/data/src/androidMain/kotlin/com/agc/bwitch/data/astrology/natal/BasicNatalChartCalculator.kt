package com.agc.bwitch.data.astrology.natal

import com.agc.bwitch.domain.astrology.natal.BirthData
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.longitudeToZodiacSign
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.sunPosition

class BasicNatalChartCalculator {
    fun calculate(birthData: BirthData): NatalChartResult {
        val time = Time(
            birthData.year,
            birthData.month,
            birthData.day,
            birthData.hour,
            birthData.minute,
            birthData.second,
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
