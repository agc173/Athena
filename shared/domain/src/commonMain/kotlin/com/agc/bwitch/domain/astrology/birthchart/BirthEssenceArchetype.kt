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

    private val displayNameEs: String
        get() = when (this) {
            MISTICA -> "La Mística"
            GUERRERA -> "La Guerrera"
            SANADORA -> "La Sanadora"
            VIDENTE -> "La Vidente"
            ALQUIMISTA -> "La Alquimista"
            GUARDIANA -> "La Guardiana"
        }

    fun displayName(languageCode: String): String {
        val normalized = languageCode.trim().lowercase()
        return when (this) {
            MISTICA -> when (normalized) {
                "en" -> "The Mystic"
                "pt" -> "A Mística"
                "ru" -> "Мистик"
                "fr" -> "La Mystique"
                "it" -> "La Mistica"
                "de" -> "Die Mystikerin"
                else -> displayNameEs
            }

            GUERRERA -> when (normalized) {
                "en" -> "The Warrior"
                "pt" -> "A Guerreira"
                "ru" -> "Воительница"
                "fr" -> "La Guerrière"
                "it" -> "La Guerriera"
                "de" -> "Die Kriegerin"
                else -> displayNameEs
            }

            SANADORA -> when (normalized) {
                "en" -> "The Healer"
                "pt" -> "A Curadora"
                "ru" -> "Целительница"
                "fr" -> "La Guérisseuse"
                "it" -> "La Guaritrice"
                "de" -> "Die Heilerin"
                else -> displayNameEs
            }

            VIDENTE -> when (normalized) {
                "en" -> "The Seer"
                "pt" -> "A Vidente"
                "ru" -> "Провидица"
                "fr" -> "La Voyante"
                "it" -> "La Veggente"
                "de" -> "Die Seherin"
                else -> displayNameEs
            }

            ALQUIMISTA -> when (normalized) {
                "en" -> "The Alchemist"
                "pt" -> "A Alquimista"
                "ru" -> "Алхимик"
                "fr" -> "L’Alchimiste"
                "it" -> "L’Alchimista"
                "de" -> "Die Alchemistin"
                else -> displayNameEs
            }

            GUARDIANA -> when (normalized) {
                "en" -> "The Guardian"
                "pt" -> "A Guardiã"
                "ru" -> "Хранительница"
                "fr" -> "La Gardienne"
                "it" -> "La Custode"
                "de" -> "Die Hüterin"
                else -> displayNameEs
            }
        }
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
