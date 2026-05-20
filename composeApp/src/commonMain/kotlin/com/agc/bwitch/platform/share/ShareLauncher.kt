package com.agc.bwitch.platform.share

import androidx.compose.runtime.Composable

/**
 * Common payload for text sharing across modules.
 */
data class ShareTextPayload(
    val text: String,
    val title: String? = null,
)

sealed interface ShareResult {
    data object Success : ShareResult
    data class Error(val message: String? = null) : ShareResult
}

expect class ShareLauncher {
    suspend fun shareText(payload: ShareTextPayload): ShareResult
}

@Composable
expect fun rememberShareLauncher(): ShareLauncher
