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
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberBirthEssenceShareLauncher(): BirthEssenceShareLauncher {
    val appContext = LocalContext.current.applicationContext
    val rootView = LocalView.current.rootView
    return remember(appContext, rootView) { AndroidBirthEssenceShareLauncher(appContext, rootView) }
}

private class AndroidBirthEssenceShareLauncher(
    private val context: Context,
    private val rootView: View,
) : BirthEssenceShareLauncher {
    override fun share(_essence: BirthEssenceProfile, captureBounds: ShareCaptureBounds): Result<Unit> = runCatching {
        val bitmap = captureAttachedCardBitmap(rootView, captureBounds)
        val imageUri = persistBitmapInCache(context, bitmap)
        launchNativeShare(context, imageUri)
    }
}

private fun captureAttachedCardBitmap(rootView: View, captureBounds: ShareCaptureBounds): Bitmap {
    check(rootView.width > 0 && rootView.height > 0) {
        "La pantalla aún no está lista para capturar"
    }
    check(captureBounds.width > 0 && captureBounds.height > 0) {
        "La carta de esencia no tiene tamaño válido para compartir"
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
        "No fue posible determinar el área visible para compartir"
    }

    return Bitmap.createBitmap(
        windowBitmap,
        sourceRect.left,
        sourceRect.top,
        sourceRect.width(),
        sourceRect.height(),
    )
}

private fun persistBitmapInCache(context: Context, bitmap: Bitmap): Uri {
    val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
    val outputFile = File(shareDir, "birth_essence_${System.currentTimeMillis()}.png")

    FileOutputStream(outputFile).use { stream ->
        val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        check(compressed) { "No se pudo serializar la imagen para compartir" }
    }

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        outputFile,
    )
}

private fun launchNativeShare(context: Context, imageUri: Uri) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, imageUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooserIntent = Intent.createChooser(shareIntent, "Compartir esencia")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    context.startActivity(chooserIntent)
}
