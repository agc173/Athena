package com.agc.bwitch.ui.astrology

import com.agc.bwitch.data.astrology.natal.BasicNatalChartCalculator
import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.NatalChartResult

actual object BasicNatalChartUiCalculator {
    private val calculator = BasicNatalChartCalculator()

    actual fun calculate(birthDateTimeUtc: BirthDateTimeUtc, birthLocation: BirthLocation?): NatalChartResult =
        calculator.calculate(birthDateTimeUtc, birthLocation)
}
