package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class BirthEssenceInput(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
)

data class BirthEssenceReading(
    val interpretation: String,
    val archetype: String? = null,
)

data class BirthEssenceDraft(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
    val interpretation: String,
    val archetype: String? = null,
)

data class BirthEssenceProfile(
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign,
    val risingSign: ZodiacSign,
    val interpretation: String,
    val archetype: String? = null,
    val savedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
)
