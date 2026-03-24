package com.agc.bwitch.ui.common

import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.major_01_magician
import bwitch.composeapp.generated.resources.major_02_high_priestess
import bwitch.composeapp.generated.resources.major_07_chariot
import bwitch.composeapp.generated.resources.major_09_hermit
import bwitch.composeapp.generated.resources.major_14_temperance
import bwitch.composeapp.generated.resources.major_19_sun
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import org.jetbrains.compose.resources.DrawableResource

fun BirthEssenceArchetype.toVisualResource(): DrawableResource = when (this) {
    BirthEssenceArchetype.MISTICA -> Res.drawable.major_02_high_priestess
    BirthEssenceArchetype.GUERRERA -> Res.drawable.major_07_chariot
    BirthEssenceArchetype.SANADORA -> Res.drawable.major_14_temperance
    BirthEssenceArchetype.VIDENTE -> Res.drawable.major_09_hermit
    BirthEssenceArchetype.ALQUIMISTA -> Res.drawable.major_01_magician
    BirthEssenceArchetype.GUARDIANA -> Res.drawable.major_19_sun
}
