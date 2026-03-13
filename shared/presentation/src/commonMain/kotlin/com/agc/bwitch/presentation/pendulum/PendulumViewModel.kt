package com.agc.bwitch.presentation.pendulum

import com.agc.bwitch.domain.pendulum.PendulumAnswer
import kotlin.random.Random
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PendulumViewModel {
    private val _uiState = MutableStateFlow(PendulumUiState())
    val uiState: StateFlow<PendulumUiState> = _uiState.asStateFlow()

    fun onQuestionChange(value: String) {
        _uiState.update { it.copy(question = value) }
    }

    fun startSwing() {
        if (_uiState.value.phase == PendulumPhase.ANIMATING) return

        val selectedAnswer = randomAnswer()
        _uiState.update {
            it.copy(
                phase = PendulumPhase.ANIMATING,
                selectedAnswer = selectedAnswer,
            )
        }
    }

    fun onSwingFinished() {
        _uiState.update { current ->
            if (current.phase != PendulumPhase.ANIMATING) return@update current
            current.copy(phase = PendulumPhase.RESULT)
        }
    }

    fun reset() {
        _uiState.update {
            PendulumUiState(
                question = "",
                phase = PendulumPhase.IDLE,
                selectedAnswer = null,
            )
        }
    }

    private fun randomAnswer(): PendulumAnswer {
        val value = Random.nextInt(100)
        return when {
            value < 35 -> PendulumAnswer.YES
            value < 70 -> PendulumAnswer.NO
            value < 85 -> PendulumAnswer.MAYBE
            else -> PendulumAnswer.NOT_NOW
        }
    }
}
