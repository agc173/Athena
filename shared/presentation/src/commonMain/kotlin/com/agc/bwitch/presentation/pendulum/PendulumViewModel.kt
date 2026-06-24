package com.agc.bwitch.presentation.pendulum

import com.agc.bwitch.domain.security.InputPolicy
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.domain.pendulum.PendulumAnswer
import kotlin.random.Random
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PendulumViewModel(
    private val economyRepository: EconomyRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val _uiState = MutableStateFlow(PendulumUiState())
    val uiState: StateFlow<PendulumUiState> = _uiState.asStateFlow()
    private val _uiEffects = MutableSharedFlow<PendulumUiEffect>(extraBufferCapacity = 16)
    val uiEffects: SharedFlow<PendulumUiEffect> = _uiEffects.asSharedFlow()

    fun onQuestionChange(value: String) {
        val limitedQuestion = InputPolicy.normalizeFreeTextInput(value, InputPolicy.ORACLE_QUESTION_MAX_LENGTH)
        _uiState.update { it.copy(question = limitedQuestion, error = null) }
    }

    fun startSwing() {
        if (_uiState.value.phase == PendulumPhase.ANIMATING) return
        val questionSnapshot = _uiState.value.question
        scope.launch {
            val auth = runCatching { economyRepository.authorizePendulum(generateRequestId(), null) }
            val authorized = auth.getOrNull()?.authorized == true
            emitDeckUnlockRewardsIfNeeded(auth.getOrNull()?.deckCardUnlockRewards.orEmpty())
            if (!authorized) {
                val normalized = (auth.exceptionOrNull()?.message ?: auth.getOrNull()?.status.orEmpty()).lowercase()
                _uiState.update { it.copy(error = when {
                    normalized.contains("daily_limit") ||
                        normalized.contains("pendulum_daily_limit_reached") ||
                        normalized.contains("pack_limit") ||
                        normalized.contains("pendulum_pack_limit_reached") -> "daily_limit"
                    normalized.contains("insufficient_moons") || normalized.contains("insufficient_moon_balance") || normalized.contains("moon") -> "insufficient_moons"
                    else -> "insufficient_moons"
                }) }
                return@launch
            }
            val selectedAnswer = randomAnswer()
            _uiState.update { current ->
                val resetQuestion = if (current.phase == PendulumPhase.RESULT) "" else questionSnapshot
                current.copy(question = resetQuestion, phase = PendulumPhase.ANIMATING, selectedAnswer = selectedAnswer, error = null)
            }
        }
    }

    fun onSwingFinished() { _uiState.update { if (it.phase != PendulumPhase.ANIMATING) it else it.copy(phase = PendulumPhase.RESULT) } }
    fun reset() { _uiState.update { PendulumUiState() } }
    private fun randomAnswer(): PendulumAnswer { val value = Random.nextInt(100); return when { value < 35 -> PendulumAnswer.YES; value < 70 -> PendulumAnswer.NO; value < 85 -> PendulumAnswer.MAYBE; else -> PendulumAnswer.NOT_NOW } }
    @OptIn(ExperimentalUuidApi::class) private fun generateRequestId(): String = Uuid.random().toString()

    private fun emitDeckUnlockRewardsIfNeeded(rewards: List<DeckCardUnlockReward>) {
        if (rewards.isEmpty()) return
        _uiEffects.tryEmit(PendulumUiEffect.ShowDeckCardUnlockRewards(rewards))
    }
}

sealed interface PendulumUiEffect {
    data class ShowDeckCardUnlockRewards(
        val rewards: List<DeckCardUnlockReward>,
    ) : PendulumUiEffect
}
