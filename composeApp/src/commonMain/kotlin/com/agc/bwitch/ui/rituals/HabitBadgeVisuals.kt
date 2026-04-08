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

    HabitBadgeType.Laurel -> HabitBadgeVisual(
        assetKey = "habit_badge_laurel",
        fallbackGlyph = "❦",
    )

    HabitBadgeType.Bird -> HabitBadgeVisual(
        assetKey = "habit_badge_bird",
        fallbackGlyph = "🕊",
    )

    HabitBadgeType.Mountain -> HabitBadgeVisual(
        assetKey = "habit_badge_mountain",
        fallbackGlyph = "⛰",
    )

    HabitBadgeType.Cloud -> HabitBadgeVisual(
        assetKey = "habit_badge_cloud",
        fallbackGlyph = "☁",
    )

    HabitBadgeType.Wave -> HabitBadgeVisual(
        assetKey = "habit_badge_wave",
        fallbackGlyph = "〰",
    )

    HabitBadgeType.Flame -> HabitBadgeVisual(
        assetKey = "habit_badge_flame",
        fallbackGlyph = "🔥",
    )

    HabitBadgeType.Cat -> HabitBadgeVisual(
        assetKey = "habit_badge_cat",
        fallbackGlyph = "🐈",
    )

    HabitBadgeType.Katrina -> HabitBadgeVisual(
        assetKey = "habit_badge_katrina",
        fallbackGlyph = "🌀",
    )
}
