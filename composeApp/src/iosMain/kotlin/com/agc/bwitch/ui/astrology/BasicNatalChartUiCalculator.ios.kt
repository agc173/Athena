package com.agc.bwitch.ui.astrology

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.NatalChartResult

actual object BasicNatalChartUiCalculator {
    actual fun calculate(birthDateTimeUtc: BirthDateTimeUtc): NatalChartResult =
        error("Basic natal chart calculation is currently available on Android only.")
}
