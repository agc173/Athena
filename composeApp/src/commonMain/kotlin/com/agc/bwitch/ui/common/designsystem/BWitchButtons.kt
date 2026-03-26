package com.agc.bwitch.ui.common.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun BWitchPrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val dimens = BWitchThemeTokens.dimens

    Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = dimens.buttonMinHeight),
        enabled = enabled,
        shape = RoundedCornerShape(dimens.buttonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = dimens.elevationXs),
        content = content,
    )
}

@Composable
fun BWitchSecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val extras = BWitchThemeTokens.extras
    val dimens = BWitchThemeTokens.dimens

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = dimens.buttonMinHeight),
        enabled = enabled,
        shape = RoundedCornerShape(dimens.buttonCornerRadius),
        border = BorderStroke(
            width = 1.dp,
            color = extras.borderSubtle,
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            containerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        content = content,
    )
}
