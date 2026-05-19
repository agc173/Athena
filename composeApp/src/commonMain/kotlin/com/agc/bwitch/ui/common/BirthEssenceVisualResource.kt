package com.agc.bwitch.ui.common

import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.birth_essence_archetype_alquimista
import bwitch.composeapp.generated.resources.birth_essence_archetype_guardiana
import bwitch.composeapp.generated.resources.birth_essence_archetype_guerrera
import bwitch.composeapp.generated.resources.birth_essence_archetype_mistica
import bwitch.composeapp.generated.resources.birth_essence_archetype_sanadora
import bwitch.composeapp.generated.resources.birth_essence_archetype_vidente
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import org.jetbrains.compose.resources.DrawableResource

fun BirthEssenceArchetype.toVisualResource(): DrawableResource = when (this) {
    BirthEssenceArchetype.MISTICA -> Res.drawable.birth_essence_archetype_mistica
    BirthEssenceArchetype.GUERRERA -> Res.drawable.birth_essence_archetype_guerrera
    BirthEssenceArchetype.SANADORA -> Res.drawable.birth_essence_archetype_sanadora
    BirthEssenceArchetype.VIDENTE -> Res.drawable.birth_essence_archetype_vidente
    BirthEssenceArchetype.ALQUIMISTA -> Res.drawable.birth_essence_archetype_alquimista
    BirthEssenceArchetype.GUARDIANA -> Res.drawable.birth_essence_archetype_guardiana
}
