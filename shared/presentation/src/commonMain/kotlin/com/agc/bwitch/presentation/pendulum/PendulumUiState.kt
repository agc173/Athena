package com.agc.bwitch.presentation.pendulum

import com.agc.bwitch.domain.pendulum.PendulumAnswer

enum class PendulumPhase {
    IDLE,
    ANIMATING,
    RESULT,
}

data class PendulumUiState(
    val question: String = "",
    val phase: PendulumPhase = PendulumPhase.IDLE,
    val selectedAnswer: PendulumAnswer? = null,
    val error: String? = null,
)
