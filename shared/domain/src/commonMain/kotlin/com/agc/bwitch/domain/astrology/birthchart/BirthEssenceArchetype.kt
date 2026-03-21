package com.agc.bwitch.domain.astrology.birthchart

import kotlinx.serialization.Serializable

@Serializable
enum class BirthEssenceArchetype {
    MISTICA,
    GUERRERA,
    SANADORA,
    VIDENTE,
    ALQUIMISTA,
    GUARDIANA;

    val displayNameEs: String
        get() = when (this) {
            MISTICA -> "La Mística"
            GUERRERA -> "La Guerrera"
            SANADORA -> "La Sanadora"
            VIDENTE -> "La Vidente"
            ALQUIMISTA -> "La Alquimista"
            GUARDIANA -> "La Guardiana"
        }

    companion object {
        fun fromRawOrNull(raw: String?): BirthEssenceArchetype? {
            if (raw.isNullOrBlank()) return null
            val normalized = raw.trim().uppercase()
            return when (normalized) {
                "MISTICA", "LA MISTICA", "LA MÍSTICA" -> MISTICA
                "GUERRERA", "LA GUERRERA" -> GUERRERA
                "SANADORA", "LA SANADORA" -> SANADORA
                "VIDENTE", "LA VIDENTE" -> VIDENTE
                "ALQUIMISTA", "LA ALQUIMISTA" -> ALQUIMISTA
                "GUARDIANA", "LA GUARDIANA" -> GUARDIANA
                else -> runCatching { valueOf(normalized) }.getOrNull()
            }
        }
    }
}
