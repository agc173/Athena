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
    val screenHorizontalPadding: Dp = 20.dp,
    val screenVerticalPadding: Dp = 16.dp,
    val sectionSpacing: Dp = 20.dp,
    val radiusSm: Dp = 8.dp,
    val radiusMd: Dp = 12.dp,
    val radiusLg: Dp = 18.dp,
    val radiusXl: Dp = 24.dp,
    val topBarCornerRadius: Dp = 20.dp,
    val textFieldCornerRadius: Dp = 14.dp,
    val buttonCornerRadius: Dp = 14.dp,
    val elevationXs: Dp = 0.dp,
    val elevationSm: Dp = 1.dp,
    val elevationMd: Dp = 3.dp,
    val elevationLg: Dp = 8.dp,
    val topBarHeight: Dp = 60.dp,
    val topBarHorizontalPadding: Dp = 8.dp,
    val topBarTitleStartPadding: Dp = 4.dp,
    val topBarBackIconSize: Dp = 24.dp,
    val buttonMinHeight: Dp = 46.dp,
    val textFieldMinHeight: Dp = 52.dp,
    val bottomBarHeight: Dp = 68.dp,
    val bottomBarHorizontalPadding: Dp = 16.dp,
)

val DefaultBWitchDimens = BWitchDimens()
