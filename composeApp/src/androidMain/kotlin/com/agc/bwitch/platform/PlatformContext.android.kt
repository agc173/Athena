package com.agc.bwitch.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberPlatformContext(): PlatformContext = LocalContext.current