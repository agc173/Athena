package com.agc.bwitch.ui.astrology

import androidx.compose.runtime.Composable
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.localization.BirthChartStrings

data class ShareCaptureBounds(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

interface BirthEssenceShareLauncher {
    fun share(essence: BirthEssenceProfile, captureBounds: ShareCaptureBounds): Result<Unit>
}

@Composable
expect fun rememberBirthEssenceShareLauncher(strings: BirthChartStrings): BirthEssenceShareLauncher
