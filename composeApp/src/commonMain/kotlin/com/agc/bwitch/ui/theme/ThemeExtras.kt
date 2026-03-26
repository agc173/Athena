package com.agc.bwitch.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class BWitchThemeExtras(
    val screenBackground: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val accentSoft: Color,
    val accentGold: Color,
    val textSecondary: Color,
    val borderSubtle: Color,
    val scrim: Color,
    val glowAlpha: Float,
    val topBarContainer: Color,
    val topBarDivider: Color,
    val topBarIconColor: Color,
    val navBarContainer: Color,
    val navBarBorder: Color,
)

val LocalBWitchThemeExtras = compositionLocalOf {
    BWitchThemeExtras(
        screenBackground = PearlBackground,
        surfaceElevated = PearlSurfaceElevated,
        surfaceMuted = PearlSurfaceContainer,
        accentSoft = AquaAccent,
        accentGold = PremiumGoldAccent,
        textSecondary = WarmTextSecondary,
        borderSubtle = NeutralBorderSubtle,
        scrim = SoftScrim,
        glowAlpha = 0.14f,
        topBarContainer = PearlSurface,
        topBarDivider = NeutralBorderSubtle,
        topBarIconColor = WarmTextPrimary,
        navBarContainer = PearlSurface,
        navBarBorder = NeutralBorderSubtle,
    )
}
