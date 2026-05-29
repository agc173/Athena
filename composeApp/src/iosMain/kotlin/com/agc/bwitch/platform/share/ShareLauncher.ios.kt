package com.agc.bwitch.platform.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIViewController

actual class ShareLauncher {
    actual suspend fun shareText(payload: ShareTextPayload): ShareResult {
        if (payload.text.isBlank()) {
            return ShareResult.Error("Cannot share empty text")
        }

        val presenter = topViewController()
            ?: return ShareResult.Error("Cannot open share sheet")

        return runCatching {
            val activityViewController = UIActivityViewController(
                activityItems = listOf(payload.text),
                applicationActivities = null,
            )
            activityViewController.popoverPresentationController?.sourceView = presenter.view
            presenter.presentViewController(
                activityViewController,
                animated = true,
                completion = null,
            )
        }.fold(
            onSuccess = { ShareResult.Success },
            onFailure = { ShareResult.Error(it.message) },
        )
    }
}

@Composable
actual fun rememberShareLauncher(): ShareLauncher = remember { ShareLauncher() }

private fun topViewController(): UIViewController? {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return null
    return root.topMost()
}

private fun UIViewController.topMost(): UIViewController {
    var top = this
    while (true) {
        val presented = top.presentedViewController ?: break
        top = presented
    }
    return top
}
