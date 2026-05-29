package com.agc.bwitch.ui.common.share.visual

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.agc.bwitch.ui.theme.BWitchThemeTokens

private const val DEFAULT_APP_NAME = "ATHENA"

@Composable
fun AthenaShareWatermark(
    appName: String,
    modifier: Modifier = Modifier,
    tagline: String? = null,
) {
    val spacing = BWitchThemeTokens.dimens
    val normalizedAppName = appName.trim().ifEmpty { DEFAULT_APP_NAME }
    val normalizedTagline = tagline?.trim()?.takeIf { it.isNotEmpty() }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.spacingXs),
    ) {
        Text(
            text = normalizedAppName.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
            letterSpacing = 1.8.sp,
            textAlign = TextAlign.Center,
        )
        normalizedTagline?.let { text ->
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.48f),
                textAlign = TextAlign.Center,
            )
        }
    }
}
