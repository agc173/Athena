package com.agc.bwitch.ui.common.designsystem

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun BWitchTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    label: @Composable (() -> Unit)? = null,
) {
    val extras = BWitchThemeTokens.extras
    val dimens = BWitchThemeTokens.dimens

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.defaultMinSize(minHeight = dimens.textFieldMinHeight),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        visualTransformation = visualTransformation,
        label = label,
        shape = RoundedCornerShape(dimens.textFieldCornerRadius),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = extras.borderSubtle,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            disabledBorderColor = extras.borderSubtle,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}
