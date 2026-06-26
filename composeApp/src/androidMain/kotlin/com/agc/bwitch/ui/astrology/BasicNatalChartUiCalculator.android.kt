package com.agc.bwitch.ui.astrology

import com.agc.bwitch.data.astrology.natal.BasicNatalChartCalculator
import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.NatalChartResult

actual object BasicNatalChartUiCalculator {
    private val calculator = BasicNatalChartCalculator()

    actual fun calculate(birthDateTimeUtc: BirthDateTimeUtc): NatalChartResult =
        calculator.calculate(birthDateTimeUtc)
}
