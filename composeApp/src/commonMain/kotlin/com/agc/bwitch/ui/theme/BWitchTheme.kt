package com.agc.bwitch.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val BWitchColorScheme = lightColorScheme(
    primary = AquaPrimary,
    onPrimary = PearlSurface,
    primaryContainer = AquaAccent,
    onPrimaryContainer = WarmTextPrimary,
    secondary = AquaPrimaryStrong,
    onSecondary = PearlSurface,
    secondaryContainer = PearlSurfaceContainer,
    onSecondaryContainer = WarmTextPrimary,
    tertiary = PremiumGoldAccent,
    onTertiary = WarmTextPrimary,
    background = PearlBackground,
    onBackground = WarmTextPrimary,
    surface = PearlSurface,
    onSurface = WarmTextPrimary,
    surfaceVariant = PearlSurfaceElevated,
    onSurfaceVariant = WarmTextSecondary,
    outline = NeutralBorderSubtle,
    error = WarmError,
    onError = PearlSurface,
    scrim = SoftScrim,
)

private val LocalBWitchDimens = staticCompositionLocalOf { DefaultBWitchDimens }

@Composable
fun BWitchTheme(
    content: @Composable () -> Unit,
) {
    val dimens = DefaultBWitchDimens
    val displayFont = bwitchDisplayFontFamily()

    CompositionLocalProvider(
        LocalBWitchThemeExtras provides BWitchThemeExtras(
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
            topBarIconColor = WarmTextPrimary.copy(alpha = 0.9f),
            navBarContainer = PearlSurface,
            navBarBorder = NeutralBorderSubtle,
        ),
        LocalBWitchDimens provides dimens,
    ) {
        MaterialTheme(
            colorScheme = BWitchColorScheme,
            typography = BWitchTypography.copy(
                headlineLarge = BWitchTypography.headlineLarge.copy(fontFamily = displayFont),
            ),
            shapes = Shapes(
                extraSmall = RoundedCornerShape(dimens.radiusSm),
                small = RoundedCornerShape(dimens.radiusMd),
                medium = RoundedCornerShape(dimens.radiusLg),
                large = RoundedCornerShape(dimens.radiusXl),
                extraLarge = RoundedCornerShape(dimens.radiusXl),
            ),
            content = content,
        )
    }
}

@Immutable
object BWitchThemeTokens {
    val extras: BWitchThemeExtras
        @Composable
        @ReadOnlyComposable
        get() = LocalBWitchThemeExtras.current

    val dimens: BWitchDimens
        @Composable
        @ReadOnlyComposable
        get() = LocalBWitchDimens.current
}

