package com.agc.bwitch.domain.rituals

enum class HabitBadgeType {
    Tree,
    Mandala,
    Firmament,
}

private val badgeOrder = listOf(
    HabitBadgeType.Tree,
    HabitBadgeType.Mandala,
    HabitBadgeType.Firmament,
)

fun habitBadgeTypeForCycles(completedCycles: Int): HabitBadgeType {
    val normalizedCycles = completedCycles.coerceAtLeast(0)
    return badgeOrder[normalizedCycles % badgeOrder.size]
}
