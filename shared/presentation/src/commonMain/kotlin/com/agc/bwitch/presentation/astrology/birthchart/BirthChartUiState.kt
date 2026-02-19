package com.agc.bwitch.presentation.astrology.birthchart

data class BirthChartUiState(
    val isLoading: Boolean = false,
    val dateText: String = "",
    val timeText: String = "",
    val placeText: String = "",
    val error: String? = null,
    val savedMessage: String? = null
)
