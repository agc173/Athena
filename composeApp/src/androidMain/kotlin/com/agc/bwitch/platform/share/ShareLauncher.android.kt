package com.agc.bwitch.platform.share

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

actual class ShareLauncher(
    private val context: Context,
) {
    actual suspend fun shareText(payload: ShareTextPayload): ShareResult {
        if (payload.text.isBlank()) {
            return ShareResult.Error("Cannot share empty text")
        }

        return runCatching {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, payload.text)
                payload.title?.takeIf { it.isNotBlank() }?.let { putExtra(Intent.EXTRA_TITLE, it) }
            }

            val chooserTitle = payload.title?.takeIf { it.isNotBlank() }
            val chooserIntent = Intent.createChooser(shareIntent, chooserTitle).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(chooserIntent)
        }.fold(
            onSuccess = { ShareResult.Success },
            onFailure = { ShareResult.Error(it.message) },
        )
    }
}

@Composable
actual fun rememberShareLauncher(): ShareLauncher {
    val context = LocalContext.current
    return remember(context) { ShareLauncher(context) }
}
