package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class BirthChartUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedSunSign: ZodiacSign = ZodiacSign.aries,
    val selectedMoonSign: ZodiacSign = ZodiacSign.aries,
    val selectedRisingSign: ZodiacSign = ZodiacSign.aries,
    val generatedInterpretation: String? = null,
    val generatedArchetype: String? = null,
    val hasSavedEssence: Boolean = false,
    val savedSummary: String? = null,
    val error: String? = null,
) {
    val isBusy: Boolean get() = isGenerating || isSaving || isRefreshing
    val hasGeneratedResult: Boolean get() = !generatedInterpretation.isNullOrBlank()
}
