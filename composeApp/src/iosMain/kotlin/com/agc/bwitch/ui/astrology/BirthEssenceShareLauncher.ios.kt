package com.agc.bwitch.ui.astrology

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile

@Composable
actual fun rememberBirthEssenceShareLauncher(): BirthEssenceShareLauncher =
    remember { IosBirthEssenceShareLauncher }

private data object IosBirthEssenceShareLauncher : BirthEssenceShareLauncher {
    override fun share(essence: BirthEssenceProfile): Result<Unit> =
        Result.failure(NotImplementedError("El share de esencia solo está implementado en Android"))
}
