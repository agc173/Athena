package com.agc.bwitch.presentation.rituals

import com.agc.bwitch.domain.rituals.HabitsRepository
import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.rituals.habitBadgeTypeForCycles
import com.agc.bwitch.domain.rituals.i18n.HabitsContentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HabitsViewModel(
    private val repository: HabitsRepository,
    private val dailyRitualRepository: DailyRitualRepository,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private var currentLanguage: AppLanguage = AppLanguage.fallback

    private val _uiState = MutableStateFlow(HabitsUiState())
    val uiState: StateFlow<HabitsUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            observeCurrentLanguageUseCase().collect { language ->
                currentLanguage = language
                refresh()
            }
        }
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
            val streak = dailyRitualRepository.getStreakForDate(todayDate())

            _uiState.update {
                it.copy(
                    isLoading = false,
                    progressPoints = progress.currentCyclePoints,
                    cycleTarget = progress.cycleTarget,
                    completedCycles = progress.completedCycles,
                    activeBadgeType = habitBadgeTypeForCycles(progress.completedCycles),
                    glowLevel = streak.toHabitsGlowLevel(),
                    intentions = intentions.map { intention ->
                        HabitIntentionUiModel(
                            id = intention.id,
                            title = HabitsContentRepository.resolveCompat(
                                language = currentLanguage,
                                value = intention.titleKey,
                            ),
                            actionText = HabitsContentRepository.resolveCompat(
                                language = currentLanguage,
                                value = intention.actionTextKey,
                            ),
                            isCompleted = intention.id in todayState.completedIntentionIds,
                        )
                    },
                )
            }
        }
    }

    private fun todayDate() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
}
