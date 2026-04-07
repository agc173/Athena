package com.agc.bwitch.ui.rituals

import com.agc.bwitch.domain.rituals.HabitBadgeType

data class HabitBadgeVisual(
    val assetKey: String,
    val fallbackGlyph: String,
)

fun HabitBadgeType.toHabitBadgeVisual(): HabitBadgeVisual = when (this) {
    HabitBadgeType.Tree -> HabitBadgeVisual(
        assetKey = "habit_badge_tree",
        fallbackGlyph = "🌳",
    )

    HabitBadgeType.Mandala -> HabitBadgeVisual(
        assetKey = "habit_badge_mandala",
        fallbackGlyph = "✺",
    )

    HabitBadgeType.Firmament -> HabitBadgeVisual(
        assetKey = "habit_badge_firmament",
        fallbackGlyph = "✦",
    )
}
