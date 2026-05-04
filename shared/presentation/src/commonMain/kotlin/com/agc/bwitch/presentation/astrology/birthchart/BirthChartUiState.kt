package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype

data class BirthChartUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentLanguageCode: String = "es",
    val selectedSunSign: ZodiacSign = ZodiacSign.aries,
    val selectedMoonSign: ZodiacSign = ZodiacSign.aries,
    val selectedRisingSign: ZodiacSign = ZodiacSign.aries,
    val generatedInterpretation: String? = null,
    val generatedLanguageCode: String = "es",
    val generatedArchetype: BirthEssenceArchetype? = null,
    val generatedSunSign: ZodiacSign? = null,
    val generatedMoonSign: ZodiacSign? = null,
    val generatedRisingSign: ZodiacSign? = null,
    val hasSavedEssence: Boolean = false,
    val savedSummary: String? = null,
    val error: String? = null,
) {
    val isBusy: Boolean get() = isGenerating || isSaving || isRefreshing
    val hasGeneratedResult: Boolean get() = !generatedInterpretation.isNullOrBlank()
}
