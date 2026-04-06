package com.agc.bwitch.presentation.rituals

import com.agc.bwitch.domain.rituals.DailyRitualStep
import com.agc.bwitch.domain.rituals.DailyRitualTemplate

data class DailyRitualUiState(
    val isLoading: Boolean = false,
    val ritual: DailyRitualTemplate? = null,
    val hasStarted: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentSteps: List<DailyRitualStep> = emptyList(),
    val textAnswer: String = "",
    val selectedOption: String? = null,
    val isCompleted: Boolean = false,
    val streakCount: Int = 0,
    val errorMessage: String? = null,
)
