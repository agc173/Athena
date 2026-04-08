package com.agc.bwitch.ui.rituals.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.habit_badge_bird
import bwitch.composeapp.generated.resources.habit_badge_cat
import bwitch.composeapp.generated.resources.habit_badge_cloud
import bwitch.composeapp.generated.resources.habit_badge_flame
import bwitch.composeapp.generated.resources.habit_badge_firmament
import bwitch.composeapp.generated.resources.habit_badge_katrina
import bwitch.composeapp.generated.resources.habit_badge_laurel
import bwitch.composeapp.generated.resources.habit_badge_mandala
import bwitch.composeapp.generated.resources.habit_badge_mountain
import bwitch.composeapp.generated.resources.habit_badge_tree
import bwitch.composeapp.generated.resources.habit_badge_wave
import com.agc.bwitch.domain.rituals.HabitBadgeType
import com.agc.bwitch.presentation.rituals.HabitsGlowLevel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun HabitsProgressBadge(
    badgeType: HabitBadgeType,
    currentPoints: Int,
    cycleTarget: Int,
    glowLevel: HabitsGlowLevel,
    modifier: Modifier = Modifier,
) {
    val normalizedCurrent = currentPoints.coerceAtLeast(0)
    val normalizedTarget = cycleTarget.coerceAtLeast(1)
    val progress = (normalizedCurrent.toFloat() / normalizedTarget.toFloat()).coerceIn(0f, 1f)
    val badgeAlpha = 0.18f + (progress * 0.82f)
    val badgeScale = 0.96f + (progress * 0.08f)
    val glow = glowStyleFor(glowLevel, MaterialTheme.colorScheme.primary)
    Box(
        modifier = modifier
            .graphicsLayer {
                alpha = badgeAlpha
                scaleX = badgeScale
                scaleY = badgeScale
                shadowElevation = glow.elevation.toPx()
                ambientShadowColor = glow.shadowColor
                spotShadowColor = glow.shadowColor
            }
            .background(
                color = glow.containerColor,
                shape = CircleShape,
            )
            .border(
                width = glow.borderWidth,
                color = glow.borderColor,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(resourceFor(badgeType)),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun resourceFor(type: HabitBadgeType): DrawableResource = when (type) {
    HabitBadgeType.Tree -> Res.drawable.habit_badge_tree
    HabitBadgeType.Mandala -> Res.drawable.habit_badge_mandala
    HabitBadgeType.Firmament -> Res.drawable.habit_badge_firmament
    HabitBadgeType.Laurel -> Res.drawable.habit_badge_laurel
    HabitBadgeType.Bird -> Res.drawable.habit_badge_bird
    HabitBadgeType.Mountain -> Res.drawable.habit_badge_mountain
    HabitBadgeType.Cloud -> Res.drawable.habit_badge_cloud
    HabitBadgeType.Wave -> Res.drawable.habit_badge_wave
    HabitBadgeType.Flame -> Res.drawable.habit_badge_flame
    HabitBadgeType.Cat -> Res.drawable.habit_badge_cat
    HabitBadgeType.Katrina -> Res.drawable.habit_badge_katrina
}

private data class HabitsBadgeGlowStyle(
    val containerColor: Color,
    val borderColor: Color,
    val borderWidth: androidx.compose.ui.unit.Dp,
    val elevation: androidx.compose.ui.unit.Dp,
    val shadowColor: Color,
    val glyphBoost: Float,
)

private fun glowStyleFor(level: HabitsGlowLevel, primary: Color): HabitsBadgeGlowStyle = when (level) {
    HabitsGlowLevel.Base -> HabitsBadgeGlowStyle(
        containerColor = primary.copy(alpha = 0.10f),
        borderColor = primary.copy(alpha = 0.24f),
        borderWidth = 1.dp,
        elevation = 0.dp,
        shadowColor = primary.copy(alpha = 0.18f),
        glyphBoost = 0f,
    )

    HabitsGlowLevel.Soft -> HabitsBadgeGlowStyle(
        containerColor = primary.copy(alpha = 0.14f),
        borderColor = primary.copy(alpha = 0.35f),
        borderWidth = 1.25.dp,
        elevation = 4.dp,
        shadowColor = primary.copy(alpha = 0.26f),
        glyphBoost = 0.03f,
    )

    HabitsGlowLevel.Bright -> HabitsBadgeGlowStyle(
        containerColor = primary.copy(alpha = 0.18f),
        borderColor = primary.copy(alpha = 0.48f),
        borderWidth = 1.5.dp,
        elevation = 9.dp,
        shadowColor = primary.copy(alpha = 0.34f),
        glyphBoost = 0.06f,
    )

    HabitsGlowLevel.Luminous -> HabitsBadgeGlowStyle(
        containerColor = primary.copy(alpha = 0.22f),
        borderColor = primary.copy(alpha = 0.58f),
        borderWidth = 1.75.dp,
        elevation = 14.dp,
        shadowColor = primary.copy(alpha = 0.42f),
        glyphBoost = 0.1f,
    )
}
