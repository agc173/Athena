package com.agc.bwitch.ui.astrology

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.localization.BirthChartStrings

@Composable
actual fun rememberBirthEssenceShareLauncher(strings: BirthChartStrings): BirthEssenceShareLauncher =
    remember(strings) { IosBirthEssenceShareLauncher(strings) }

private data class IosBirthEssenceShareLauncher(
    private val strings: BirthChartStrings,
) : BirthEssenceShareLauncher {
    // TODO: Implement iOS visual share capture/export.
    // Text-only fallback sharing already exists in ShareLauncher iOS.
    override fun share(essence: BirthEssenceProfile, captureBounds: ShareCaptureBounds): Result<Unit> =
        Result.failure(NotImplementedError(strings.shareNotAvailablePlatformError))
}
