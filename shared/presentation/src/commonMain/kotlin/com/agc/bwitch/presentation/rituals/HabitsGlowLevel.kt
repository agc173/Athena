package com.agc.bwitch.presentation.rituals

enum class HabitsGlowLevel {
    Base,
    Soft,
    Bright,
    Luminous,
}

fun Int.toHabitsGlowLevel(): HabitsGlowLevel = when {
    this <= 0 -> HabitsGlowLevel.Base
    this <= 2 -> HabitsGlowLevel.Soft
    this <= 6 -> HabitsGlowLevel.Bright
    else -> HabitsGlowLevel.Luminous
}
