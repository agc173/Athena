package com.agc.bwitch.ui.common.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun BWitchCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues? = null,
    contentVerticalArrangement: Arrangement.Vertical? = null,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = BWitchThemeTokens.extras.surfaceMuted,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ),
    shape: Shape = MaterialTheme.shapes.medium,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = BWitchThemeTokens.dimens
    val resolvedContentPadding = contentPadding ?: PaddingValues(dimens.spacingMd)
    val resolvedArrangement = contentVerticalArrangement ?: Arrangement.spacedBy(dimens.spacingSm)

    val body: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(resolvedContentPadding),
            verticalArrangement = resolvedArrangement,
            content = content,
        )
    }

    if (onClick != null) {
        Card(
            modifier = modifier,
            onClick = onClick,
            enabled = enabled,
            colors = colors,
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationSm),
            content = body,
        )
    } else {
        Card(
            modifier = modifier,
            colors = colors,
            shape = shape,
            elevation = CardDefaults.cardElevation(defaultElevation = dimens.elevationSm),
            content = body,
        )
    }
}
