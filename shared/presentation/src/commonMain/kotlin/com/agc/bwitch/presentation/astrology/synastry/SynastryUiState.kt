package com.agc.bwitch.presentation.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryReading

data class SynastryPersonForm(
    val sunSign: ZodiacSign? = null,
    val moonSign: ZodiacSign? = null,
    val risingSign: ZodiacSign? = null,
)

data class SynastryUiState(
    val personA: SynastryPersonForm = SynastryPersonForm(),
    val personB: SynastryPersonForm = SynastryPersonForm(),
    val reading: SynastryReading? = null,
    val isGenerating: Boolean = false,
    val error: String? = null,
) {
    val canGenerate: Boolean get() = personA.sunSign != null && personB.sunSign != null && !isGenerating
}
