package com.agc.bwitch.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class BWitchDimens(
    val spacingXs: Dp = 4.dp,
    val spacingSm: Dp = 8.dp,
    val spacingMd: Dp = 16.dp,
    val spacingLg: Dp = 24.dp,
    val spacingXl: Dp = 32.dp,
    val radiusSm: Dp = 6.dp,
    val radiusMd: Dp = 10.dp,
    val radiusLg: Dp = 16.dp,
    val radiusXl: Dp = 24.dp,
    val elevationSm: Dp = 1.dp,
    val elevationMd: Dp = 4.dp,
    val elevationLg: Dp = 8.dp,
)

val DefaultBWitchDimens = BWitchDimens()
