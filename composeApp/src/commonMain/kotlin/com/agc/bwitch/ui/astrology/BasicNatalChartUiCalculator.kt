package com.agc.bwitch.ui.astrology

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.NatalChartResult

expect object BasicNatalChartUiCalculator {
    fun calculate(birthDateTimeUtc: BirthDateTimeUtc): NatalChartResult
}
