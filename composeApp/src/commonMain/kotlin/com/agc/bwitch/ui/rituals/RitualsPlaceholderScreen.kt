package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agc.bwitch.ui.common.designsystem.BWitchScreen

@Composable
fun RitualsPlaceholderScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Rituales en construcción",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "Placeholder temporal para el módulo de Rituales.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
