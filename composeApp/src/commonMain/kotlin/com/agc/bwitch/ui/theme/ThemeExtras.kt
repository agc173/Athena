package com.agc.bwitch.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BWitchThemeExtras(
    val screenBackground: Color,
    val surfaceElevated: Color,
    val accentGold: Color,
    val textSecondary: Color,
    val borderSubtle: Color,
    val scrim: Color,
    val glowAlpha: Float,
)

val LocalBWitchThemeExtras = compositionLocalOf {
    BWitchThemeExtras(
        screenBackground = MysticBackground,
        surfaceElevated = MysticSurfaceElevated,
        accentGold = MysticAccentGold,
        textSecondary = MysticTextSecondary,
        borderSubtle = MysticBorderSubtle,
        scrim = MysticScrim,
        glowAlpha = 0.24f,
    )
}
