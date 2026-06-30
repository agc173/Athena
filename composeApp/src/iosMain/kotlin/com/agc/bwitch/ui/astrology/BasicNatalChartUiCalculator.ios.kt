package com.agc.bwitch.ui.astrology

import com.agc.bwitch.domain.astrology.natal.BirthDateTimeUtc
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.NatalChartResult

actual object BasicNatalChartUiCalculator {
    actual fun calculate(birthDateTimeUtc: BirthDateTimeUtc, birthLocation: BirthLocation?): NatalChartResult =
        error("Basic natal chart calculation is currently available on Android only.")
}
