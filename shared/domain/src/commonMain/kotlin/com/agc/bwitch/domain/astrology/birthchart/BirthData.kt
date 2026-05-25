package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.model.DeckCardUnlockReward

data class BirthEssenceInput(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
    val languageCode: String = "es",
    val archetypeHint: BirthEssenceArchetype? = null,
)

data class BirthEssenceReading(
    val interpretation: String,
    val languageCode: String = "es",
    val archetype: BirthEssenceArchetype? = null,
    val deckCardUnlockRewards: List<DeckCardUnlockReward> = emptyList(),
)

data class BirthEssenceDraft(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
    val interpretation: String,
    val languageCode: String = "es",
    val archetype: BirthEssenceArchetype? = null,
)

data class BirthEssenceProfile(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
    val interpretation: String,
    val languageCode: String = "es",
    val archetype: BirthEssenceArchetype? = null,
    val savedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
