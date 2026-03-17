package com.agc.bwitch.ui.common.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun BWitchSectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    subtitleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
    ) {
        Text(
            text = title,
            style = titleStyle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = subtitleStyle,
                color = extras.textSecondary,
            )
        }
    }
}
