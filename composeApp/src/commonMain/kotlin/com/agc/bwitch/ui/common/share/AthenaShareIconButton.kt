package com.agc.bwitch.ui.common.share

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AthenaShareIconButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.38f)

    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics {
            this.contentDescription = contentDescription
        },
    ) {
        AthenaShareGlyph(
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun AthenaShareGlyph(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.09f
        val start = Offset(size.width * 0.28f, size.height * 0.62f)
        val top = Offset(size.width * 0.72f, size.height * 0.32f)
        val bottom = Offset(size.width * 0.72f, size.height * 0.78f)
        drawLine(
            color = tint,
            start = start,
            end = top,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = start,
            end = bottom,
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        val radius = size.minDimension * 0.13f
        drawCircle(color = tint, radius = radius, center = start)
        drawCircle(color = tint, radius = radius, center = top)
        drawCircle(color = tint, radius = radius, center = bottom)
    }
}
