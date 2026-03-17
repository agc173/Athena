package com.agc.bwitch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val BWitchColorScheme = darkColorScheme(
    primary = MysticPrimary,
    onPrimary = MysticBackground,
    primaryContainer = MysticSecondary,
    onPrimaryContainer = MysticTextPrimary,
    secondary = MysticSecondary,
    onSecondary = MysticTextPrimary,
    secondaryContainer = MysticSurfaceElevated,
    onSecondaryContainer = MysticTextPrimary,
    tertiary = MysticAccentGold,
    onTertiary = MysticBackground,
    background = MysticBackground,
    onBackground = MysticTextPrimary,
    surface = MysticSurface,
    onSurface = MysticTextPrimary,
    surfaceVariant = MysticSurfaceElevated,
    onSurfaceVariant = MysticTextSecondary,
    outline = MysticBorderSubtle,
    error = MysticError,
    onError = MysticTextPrimary,
)

private val LocalBWitchDimens = staticCompositionLocalOf { DefaultBWitchDimens }

@Composable
fun BWitchTheme(
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalBWitchThemeExtras provides BWitchThemeExtras(
            screenBackground = MysticBackground,
            surfaceElevated = MysticSurfaceElevated,
            accentGold = MysticAccentGold,
            textSecondary = MysticTextSecondary,
            borderSubtle = MysticBorderSubtle,
            scrim = MysticScrim,
            glowAlpha = 0.24f,
        ),
        LocalBWitchDimens provides DefaultBWitchDimens,
    ) {
        MaterialTheme(
            colorScheme = BWitchColorScheme,
            typography = BWitchTypography,
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
