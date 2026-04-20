package com.agc.bwitch.ui.astrology

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.net.Uri
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.localization.BirthChartStrings
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberBirthEssenceShareLauncher(strings: BirthChartStrings): BirthEssenceShareLauncher {
    val appContext = LocalContext.current.applicationContext
    val rootView = LocalView.current.rootView
    return remember(appContext, rootView, strings) { AndroidBirthEssenceShareLauncher(appContext, rootView, strings) }
}

private class AndroidBirthEssenceShareLauncher(
    private val context: Context,
    private val rootView: View,
    private val strings: BirthChartStrings,
) : BirthEssenceShareLauncher {
    override fun share(_essence: BirthEssenceProfile, captureBounds: ShareCaptureBounds): Result<Unit> = runCatching {
        val bitmap = captureAttachedCardBitmap(rootView, captureBounds, strings)
        val imageUri = persistBitmapInCache(context, bitmap, strings)
        launchNativeShare(context, imageUri, strings)
    }
}

private fun captureAttachedCardBitmap(
    rootView: View,
    captureBounds: ShareCaptureBounds,
    strings: BirthChartStrings,
): Bitmap {
    check(rootView.width > 0 && rootView.height > 0) {
        strings.shareCaptureScreenNotReadyError
    }
    check(captureBounds.width > 0 && captureBounds.height > 0) {
        strings.shareCaptureInvalidCardSizeError
    }

    val windowBitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
    rootView.draw(Canvas(windowBitmap))

    val sourceRect = Rect(
        captureBounds.left.coerceIn(0, rootView.width - 1),
        captureBounds.top.coerceIn(0, rootView.height - 1),
        (captureBounds.left + captureBounds.width).coerceIn(1, rootView.width),
        (captureBounds.top + captureBounds.height).coerceIn(1, rootView.height),
    )
    check(sourceRect.width() > 0 && sourceRect.height() > 0) {
        strings.shareCaptureVisibleAreaError
    }

    return Bitmap.createBitmap(
        windowBitmap,
        sourceRect.left,
        sourceRect.top,
        sourceRect.width(),
        sourceRect.height(),
    )
}

private fun persistBitmapInCache(context: Context, bitmap: Bitmap, strings: BirthChartStrings): Uri {
    val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val outputFile = File(shareDir, "birth_essence_${System.currentTimeMillis()}.png")

    FileOutputStream(outputFile).use { stream ->
        val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        check(compressed) { strings.shareSerializeImageError }
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile,
    )
}

private fun launchNativeShare(context: Context, imageUri: Uri, strings: BirthChartStrings) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, strings.shareChooserTitle)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(chooserIntent)
}
