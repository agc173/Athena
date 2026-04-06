package com.agc.bwitch.presentation.rituals

import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.rituals.DailyRitualStepKind
import com.agc.bwitch.domain.rituals.dailyRitualBranchKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class DailyRitualViewModel(
    private val repository: DailyRitualRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(DailyRitualUiState(isLoading = true))
    val uiState: StateFlow<DailyRitualUiState> = _uiState.asStateFlow()

    init {
        loadTodayRitual()
        startDailyRolloverWatcher()
    }

    fun onScreenVisible() {
        refreshIfDateChanged()
    }

    fun onStartRitual() {
        if (refreshIfDateChanged()) return
        val ritual = _uiState.value.ritual ?: return
        _uiState.update {
            it.copy(
                hasStarted = true,
                currentStepIndex = 0,
                currentSteps = ritual.steps,
                selectedOption = null,
                textAnswer = "",
                errorMessage = null,
            )
        }
    }

    fun onTextAnswerChange(value: String) {
        _uiState.update { it.copy(textAnswer = value, errorMessage = null) }
    }

    fun onOptionSelected(value: String) {
        _uiState.update { it.copy(selectedOption = value, errorMessage = null) }
    }

    fun onContinue() {
        if (refreshIfDateChanged()) return
        val state = _uiState.value
        if (state.isCompleted || !state.hasStarted) return

        val currentStep = state.currentSteps.getOrNull(state.currentStepIndex) ?: run {
            completeRitual()
            return
        }

        val validationError = validateStep(currentStep.kind, state.textAnswer, state.selectedOption)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        val withBranch = applyBranchIfNeeded(state)
        val nextIndex = withBranch.currentStepIndex + 1
        if (nextIndex >= withBranch.currentSteps.size) {
            completeRitual()
            return
        }

        _uiState.update {
            it.copy(
                currentStepIndex = nextIndex,
                textAnswer = "",
                selectedOption = null,
                errorMessage = null,
            )
        }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadTodayRitual() {
        scope.launch {
            val today = todayDate()
            val ritual = repository.getTemplateForDate(today)
            val streak = repository.getStreakForDate(today)
            val completedToday = repository.isCompletedOn(today)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    ritualDate = today,
                    ritual = ritual,
                    streakCount = streak,
                    isCompleted = completedToday,
                    hasStarted = false,
                    currentStepIndex = 0,
                    currentSteps = emptyList(),
                    textAnswer = "",
                    selectedOption = null,
                    errorMessage = null,
                )
            }
        }
    }

    private fun startDailyRolloverWatcher() {
        scope.launch {
            while (true) {
                delay(60_000)
                refreshIfDateChanged()
            }
        }
    }

    private fun refreshIfDateChanged(): Boolean {
        val today = todayDate()
        if (_uiState.value.ritualDate == today) return false
        loadTodayRitual()
        return true
    }

    private fun completeRitual() {
        val today = todayDate()
        val newStreak = repository.completeOn(today)
        _uiState.update {
            it.copy(
                isCompleted = true,
                hasStarted = false,
                streakCount = newStreak,
                textAnswer = "",
                selectedOption = null,
                errorMessage = null,
            )
        }
    }

    private fun applyBranchIfNeeded(state: DailyRitualUiState): DailyRitualUiState {
        val ritual = state.ritual ?: return state
        val currentStep = state.currentSteps.getOrNull(state.currentStepIndex) ?: return state

        if (currentStep.kind != DailyRitualStepKind.BinaryChoice) return state
        val selected = state.selectedOption ?: return state

        val branch = ritual.branches[dailyRitualBranchKey(currentStep.id, selected)] ?: return state

        val updatedSteps = state.currentSteps.take(state.currentStepIndex + 1) + branch

        _uiState.update { current ->
            current.copy(
                currentSteps = updatedSteps,
                errorMessage = null,
            )
        }

        return state.copy(currentSteps = updatedSteps)
    }

    private fun validateStep(
        kind: DailyRitualStepKind,
        textAnswer: String,
        selectedOption: String?,
    ): String? = when (kind) {
        DailyRitualStepKind.TextInput -> if (textAnswer.isBlank()) "Escribe una respuesta para continuar." else null
        DailyRitualStepKind.SingleChoice,
        DailyRitualStepKind.BinaryChoice,
        -> if (selectedOption == null) "Selecciona una opción para continuar." else null

        DailyRitualStepKind.Info,
        DailyRitualStepKind.Confirmation,
        -> null
    }

    private fun todayDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
