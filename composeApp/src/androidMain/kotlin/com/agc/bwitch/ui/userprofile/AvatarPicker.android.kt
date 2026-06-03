package com.agc.bwitch.ui.userprofile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.media.ExifInterface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.agc.bwitch.localization.ProfileStrings
import com.agc.bwitch.localization.appStrings
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

private const val AvatarOutputSizePx = 512
private const val MaxAvatarBytes = 5L * 1024L * 1024L
private val SupportedAvatarMimeTypes = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

@Composable
actual fun AvatarPickerButton(
    enabled: Boolean,
    buttonText: String,
    onPicked: (uriString: String, mimeType: String?) -> Unit
) {
    val context: Context = LocalContext.current
    val strings = appStrings.profile
    var pendingAvatar by remember { mutableStateOf<PendingAvatar?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri)?.lowercase()
        pendingAvatar = PendingAvatar(uri = uri, mimeType = mime)
    }

    Button(
        onClick = { launcher.launch("image/*") },
        enabled = enabled
    ) {
        Text(buttonText)
    }

    pendingAvatar?.let { avatar ->
        AvatarCropDialog(
            pendingAvatar = avatar,
            strings = strings,
            onDismiss = { pendingAvatar = null },
            onCropped = { croppedFile ->
                pendingAvatar = null
                onPicked(Uri.fromFile(croppedFile).toString(), "image/jpeg")
            }
        )
    }
}

@Composable
private fun AvatarCropDialog(
    pendingAvatar: PendingAvatar,
    strings: ProfileStrings,
    onDismiss: () -> Unit,
    onCropped: (File) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val cropSizeDp = 280.dp
    val cropSizePx = with(density) { cropSizeDp.toPx() }

    var bitmap by remember(pendingAvatar.uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(pendingAvatar.uri) { mutableStateOf<String?>(null) }
    var userScale by remember(pendingAvatar.uri) { mutableFloatStateOf(1f) }
    var offset by remember(pendingAvatar.uri) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(pendingAvatar) {
        error = validatePickedAvatar(context, pendingAvatar, strings)
        if (error == null) {
            bitmap = decodePickedBitmap(context, pendingAvatar.uri)
            if (bitmap == null) error = strings.avatarCropOpenError
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(strings.avatarCropTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    strings.avatarCropInstructions,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val currentBitmap = bitmap
                if (error != null) {
                    Box(
                        modifier = Modifier
                            .size(cropSizeDp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = error.orEmpty(),
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                } else if (currentBitmap == null) {
                    Box(
                        modifier = Modifier
                            .size(cropSizeDp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(strings.avatarCropLoading, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    val imageBitmap = remember(currentBitmap) { currentBitmap.asImageBitmap() }
                    fun clamp(currentOffset: Offset, currentScale: Float): Offset =
                        currentOffset.clampedFor(currentBitmap, cropSizePx, currentScale)

                    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                        val nextScale = (userScale * zoomChange).coerceIn(1f, 4f)
                        userScale = nextScale
                        offset = clamp(offset + panChange, nextScale)
                    }

                    LaunchedEffect(currentBitmap, cropSizePx, userScale) {
                        offset = clamp(offset, userScale)
                    }

                    val cropBorderColor = MaterialTheme.colorScheme.primary
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(
                            modifier = Modifier
                                .size(cropSizeDp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .transformable(transformableState)
                                .border(2.dp, cropBorderColor, CircleShape),
                        ) {
                            clipRect {
                                val placement = currentBitmap.placement(size.width, size.height, userScale, offset)
                                drawImage(
                                    image = imageBitmap,
                                    dstOffset = androidx.compose.ui.unit.IntOffset(
                                        placement.left.roundToInt(),
                                        placement.top.roundToInt(),
                                    ),
                                    dstSize = androidx.compose.ui.unit.IntSize(
                                        placement.width.roundToInt(),
                                        placement.height.roundToInt(),
                                    ),
                                    filterQuality = FilterQuality.High,
                                )
                            }
                            drawCircle(
                                color = cropBorderColor.copy(alpha = 0.95f),
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }

                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(strings.avatarCropZoomLabel, style = MaterialTheme.typography.labelMedium)
                        Slider(
                            value = userScale,
                            onValueChange = { value ->
                                userScale = value.coerceIn(1f, 4f)
                                offset = clamp(offset, userScale)
                            },
                            valueRange = 1f..4f,
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.editProfileCancel) }
                    OutlinedButton(
                        onClick = {
                            userScale = 1f
                            offset = Offset.Zero
                        },
                        enabled = bitmap != null,
                    ) { Text(strings.avatarCropCenterAction) }
                    Button(
                        onClick = {
                            val currentBitmap = bitmap ?: return@Button
                            val file = cropAvatarToTempFile(context, currentBitmap, cropSizePx, userScale, offset)
                            onCropped(file)
                        },
                        enabled = bitmap != null && error == null,
                    ) { Text(strings.avatarCropUseAction) }
                }
            }
        }
    }
}

private data class PendingAvatar(
    val uri: Uri,
    val mimeType: String?,
)

private data class ImagePlacement(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val scale: Float,
)

private fun validatePickedAvatar(context: Context, pendingAvatar: PendingAvatar, strings: ProfileStrings): String? {
    val mimeType = pendingAvatar.mimeType
    if (mimeType != null && mimeType !in SupportedAvatarMimeTypes) {
        return strings.avatarCropUnsupportedFormatError
    }

    val size = context.contentResolver.openAssetFileDescriptor(pendingAvatar.uri, "r")?.use { it.length }
    if (size != null && size > MaxAvatarBytes) {
        return strings.avatarCropMaxSizeError
    }

    return null
}

private fun decodePickedBitmap(context: Context, uri: Uri): Bitmap? {
    val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input)
    } ?: return null

    val rotationDegrees = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ExifInterface(input).rotationDegrees()
        } ?: 0
    }.getOrDefault(0)

    return bitmap.rotatedBy(rotationDegrees)
}

private fun ExifInterface.rotationDegrees(): Int = when (
    getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}

private fun Bitmap.rotatedBy(degrees: Int): Bitmap {
    if (degrees == 0) return this

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    val rotated = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (rotated != this) recycle()
    return rotated
}

private fun cropAvatarToTempFile(
    context: Context,
    bitmap: Bitmap,
    viewportPx: Float,
    userScale: Float,
    offset: Offset,
): File {
    val placement = bitmap.placement(viewportPx, viewportPx, userScale, offset)
    val sourceLeft = ((0f - placement.left) / placement.scale).roundToInt().coerceIn(0, bitmap.width - 1)
    val sourceTop = ((0f - placement.top) / placement.scale).roundToInt().coerceIn(0, bitmap.height - 1)
    val sourceSize = (viewportPx / placement.scale).roundToInt()
    val sourceRight = (sourceLeft + sourceSize).coerceIn(sourceLeft + 1, bitmap.width)
    val sourceBottom = (sourceTop + sourceSize).coerceIn(sourceTop + 1, bitmap.height)

    val output = Bitmap.createBitmap(AvatarOutputSizePx, AvatarOutputSizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(output)
    canvas.drawBitmap(
        bitmap,
        Rect(sourceLeft, sourceTop, sourceRight, sourceBottom),
        Rect(0, 0, AvatarOutputSizePx, AvatarOutputSizePx),
        android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG or android.graphics.Paint.ANTI_ALIAS_FLAG),
    )

    val file = File(context.cacheDir, "avatar_crop_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { outputStream ->
        output.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
    }
    output.recycle()
    return file
}

private fun Bitmap.placement(viewportWidth: Float, viewportHeight: Float, userScale: Float, offset: Offset): ImagePlacement {
    val scale = max(viewportWidth / width, viewportHeight / height) * userScale
    val drawWidth = width * scale
    val drawHeight = height * scale
    return ImagePlacement(
        left = (viewportWidth - drawWidth) / 2f + offset.x,
        top = (viewportHeight - drawHeight) / 2f + offset.y,
        width = drawWidth,
        height = drawHeight,
        scale = scale,
    )
}

private fun Offset.clampedFor(bitmap: Bitmap, viewportPx: Float, userScale: Float): Offset {
    val placement = bitmap.placement(viewportPx, viewportPx, userScale, Offset.Zero)
    val maxX = max(0f, (placement.width - viewportPx) / 2f)
    val maxY = max(0f, (placement.height - viewportPx) / 2f)
    return Offset(x.coerceIn(-maxX, maxX), y.coerceIn(-maxY, maxY))
}
