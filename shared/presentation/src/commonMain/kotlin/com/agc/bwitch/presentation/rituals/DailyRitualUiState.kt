package com.agc.bwitch.presentation.rituals

import com.agc.bwitch.domain.rituals.DailyRitualStep
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import kotlinx.datetime.LocalDate

enum class DailyRitualError {
    TextRequired,
    OptionRequired,
}

data class DailyRitualUiState(
    val isLoading: Boolean = false,
    val ritualDate: LocalDate? = null,
    val ritual: DailyRitualTemplate? = null,
    val hasStarted: Boolean = false,
    val currentStepIndex: Int = 0,
    val currentSteps: List<DailyRitualStep> = emptyList(),
    val textAnswer: String = "",
    val selectedOptionKey: String? = null,
    val isCompleted: Boolean = false,
    val streakCount: Int = 0,
    val error: DailyRitualError? = null,
)
