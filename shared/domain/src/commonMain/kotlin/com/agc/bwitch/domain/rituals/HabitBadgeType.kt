package com.agc.bwitch.domain.rituals

enum class HabitBadgeType {
    Tree,
    Mandala,
    Firmament,
    Laurel,
    Bird,
    Mountain,
    Cloud,
    Wave,
    Flame,
    Cat,
    Katrina,
}

val badgeOrder = listOf(
    HabitBadgeType.Tree,
    HabitBadgeType.Mandala,
    HabitBadgeType.Firmament,
    HabitBadgeType.Laurel,
    HabitBadgeType.Bird,
    HabitBadgeType.Mountain,
    HabitBadgeType.Cloud,
    HabitBadgeType.Wave,
    HabitBadgeType.Flame,
    HabitBadgeType.Cat,
    HabitBadgeType.Katrina,
)

fun habitBadgeTypeForCycles(completedCycles: Int): HabitBadgeType {
    val normalizedCycles = completedCycles.coerceAtLeast(0)
    return badgeOrder[normalizedCycles % badgeOrder.size]
}


fun completedHabitBadgesForCycles(completedCycles: Int): List<HabitBadgeType> {
    val normalizedCycles = completedCycles.coerceAtLeast(0)
    return badgeOrder.take(normalizedCycles.coerceAtMost(badgeOrder.size))
}
