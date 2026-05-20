package com.agc.bwitch.platform.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

actual class ShareLauncher {
    actual suspend fun shareText(payload: ShareTextPayload): ShareResult {
        return ShareResult.Error("Sharing is not available on this platform yet")
    }
}

@Composable
actual fun rememberShareLauncher(): ShareLauncher = remember { ShareLauncher() }
