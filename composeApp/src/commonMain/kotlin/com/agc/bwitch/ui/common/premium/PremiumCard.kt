package com.agc.bwitch.ui.common.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agc.bwitch.ui.common.designsystem.BWitchCard

@Composable
fun PremiumCard(
    title: String,
    statusLabel: String,
    primaryActionLabel: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    restoreActionLabel: String? = null,
    onPrimaryActionClick: () -> Unit,
    onRestoreActionClick: (() -> Unit)? = null,
) {
    BWitchCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onPrimaryActionClick,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(primaryActionLabel)
                }
                if (restoreActionLabel != null && onRestoreActionClick != null) {
                    OutlinedButton(
                        onClick = onRestoreActionClick,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(restoreActionLabel)
                    }
                }
            }
        }
    }
}
