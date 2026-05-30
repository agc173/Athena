package com.agc.bwitch.ui.common.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agc.bwitch.ui.common.designsystem.BWitchCard

@Composable
fun PremiumBenefitsList(
    bullets: List<String>,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    disclaimer: String? = null,
    showContainer: Boolean = true,
) {
    if (showContainer) {
        BWitchCard(modifier = modifier.fillMaxWidth()) {
            PremiumBenefitsContent(
                title = title,
                subtitle = subtitle,
                bullets = bullets,
                disclaimer = disclaimer,
            )
        }
    } else {
        PremiumBenefitsContent(
            title = title,
            subtitle = subtitle,
            bullets = bullets,
            disclaimer = disclaimer,
            modifier = modifier,
        )
    }
}

@Composable
fun PremiumBenefitsDialog(
    title: String,
    bullets: List<String>,
    closeLabel: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    disclaimer: String? = null,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            PremiumBenefitsList(
                bullets = bullets,
                subtitle = subtitle,
                disclaimer = disclaimer,
                showContainer = false,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(closeLabel)
            }
        },
    )
}

@Composable
private fun PremiumBenefitsContent(
    bullets: List<String>,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    disclaimer: String? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            bullets.forEach { bullet ->
                PremiumBenefitBullet(text = bullet)
            }
        }
        if (!disclaimer.isNullOrBlank()) {
            Text(
                text = disclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PremiumBenefitBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}
