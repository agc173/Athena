package com.agc.bwitch.presentation.astrology.birthchart

data class BirthChartUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRefreshing: Boolean = false,

    val dateText: String = "",
    val timeText: String = "",
    val placeText: String = "",

    val error: String? = null,
    val savedMessage: String? = null
) {
    val isBusy: Boolean get() = isSaving || isRefreshing
}
