package com.agc.bwitch.ui.astrology

import androidx.compose.runtime.Composable
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile

interface BirthEssenceShareLauncher {
    fun share(essence: BirthEssenceProfile): Result<Unit>
}

@Composable
expect fun rememberBirthEssenceShareLauncher(): BirthEssenceShareLauncher
