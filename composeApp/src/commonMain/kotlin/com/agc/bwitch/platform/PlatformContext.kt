package com.agc.bwitch.platform

import androidx.compose.runtime.Composable

// PlatformContext es un alias común (no tipamos a Context en commonMain)
typealias PlatformContext = Any

@Composable
expect fun rememberPlatformContext(): PlatformContext