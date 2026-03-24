package com.agc.bwitch.ui.astrology

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.ui.theme.BWitchTheme
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberBirthEssenceShareLauncher(): BirthEssenceShareLauncher {
    val appContext = LocalContext.current.applicationContext
    return remember(appContext) { AndroidBirthEssenceShareLauncher(appContext) }
}

private class AndroidBirthEssenceShareLauncher(
    private val context: Context,
) : BirthEssenceShareLauncher {
    override fun share(essence: BirthEssenceProfile): Result<Unit> = runCatching {
        val bitmap = renderCardBitmap(context, essence)
        val imageUri = persistBitmapInCache(context, bitmap)
        launchNativeShare(context, imageUri)
    }
}

private fun renderCardBitmap(context: Context, essence: BirthEssenceProfile): Bitmap {
    val composeView = ComposeView(context).apply {
        setContent {
            BWitchTheme {
                BirthEssenceShareCard(
                    essence = essence,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    val displayWidth = context.resources.displayMetrics.widthPixels
    val targetWidth = displayWidth.coerceIn(minimumValue = 720, maximumValue = 1080)

    val widthSpec = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY)
    val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

    composeView.measure(widthSpec, heightSpec)
    val measuredHeight = composeView.measuredHeight.coerceAtLeast(1)
    composeView.layout(0, 0, targetWidth, measuredHeight)

    return Bitmap.createBitmap(targetWidth, measuredHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
        composeView.draw(Canvas(bitmap))
    }
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
