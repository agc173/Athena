package com.agc.bwitch.presentation.rituals

import com.agc.bwitch.domain.rituals.HabitsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HabitsViewModel(
    private val repository: HabitsRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun onToggleIntention(intentionId: String, completed: Boolean) {
        scope.launch {
            if (completed) {
                repository.unmarkCompleted(intentionId)
            } else {
                repository.markCompleted(intentionId)
            }
            refresh()
        }
    }

    private fun refresh() {
        scope.launch {
            val todayState = repository.getTodayState()
            val progress = repository.getProgress()
            val intentions = repository.getTodayIntentions()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    progressPoints = progress.currentCyclePoints,
                    cycleTarget = progress.cycleTarget,
                    completedCycles = progress.completedCycles,
                    intentions = intentions.map { intention ->
                        HabitIntentionUiModel(
                            id = intention.id,
                            title = intention.title,
                            actionText = intention.actionText,
                            isCompleted = intention.id in todayState.completedIntentionIds,
                        )
                    },
                )
            }
        }
    }
}
