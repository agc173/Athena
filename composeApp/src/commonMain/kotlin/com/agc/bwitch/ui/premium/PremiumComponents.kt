package com.agc.bwitch.ui.premium

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agc.bwitch.localization.PremiumStrings
import com.agc.bwitch.ui.common.designsystem.BWitchCard

@Composable
fun PremiumCard(
    strings: PremiumStrings,
    priceLabel: String?,
    statusLabel: String,
    isActive: Boolean,
    isPending: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSubscribeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onManageClick: () -> Unit,
) {
    BWitchCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        PremiumPaywallContent(
            strings = strings,
            priceLabel = priceLabel,
            statusLabel = statusLabel,
            isActive = isActive,
            isPending = isPending,
            isLoading = isLoading,
            onSubscribeClick = onSubscribeClick,
            onRestoreClick = onRestoreClick,
            onManageClick = onManageClick,
        )
    }
}

@Composable
fun PremiumPaywallContent(
    strings: PremiumStrings,
    priceLabel: String?,
    statusLabel: String,
    isActive: Boolean,
    isPending: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    onSubscribeClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onManageClick: () -> Unit,
) {
    val effectivePrice = priceLabel?.takeUnless { it.isBlank() } ?: strings.monthlyPriceFallback
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = strings.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${strings.monthlySubscription} · $effectivePrice",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                )
            }
            PremiumStatusPill(statusLabel = statusLabel, isActive = isActive, isPending = isPending)
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = strings.notUnlimited,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = strings.moonsExtraUsage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
            )
        }

        PremiumBenefitsList(strings = strings)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isActive) {
                Button(
                    onClick = onManageClick,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.manageSubscription)
                }
            } else {
                Button(
                    onClick = onSubscribeClick,
                    enabled = !isLoading && !isPending,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.subscribe)
                }
            }
            OutlinedButton(
                onClick = onRestoreClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.restorePurchases)
            }
        }
    }
}

@Composable
fun PremiumBenefitsList(
    strings: PremiumStrings,
    modifier: Modifier = Modifier,
) {
    val benefits = listOf(
        strings.benefitHoroscope,
        strings.benefitBirthEssence,
        strings.benefitSynastry,
        strings.benefitTarotOne,
        strings.benefitTarotThree,
        strings.benefitOracle,
        strings.benefitPendulum,
        strings.benefitMoonsExtra,
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = strings.benefitsTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        benefits.forEach { benefit ->
            Text(
                text = "• $benefit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.9f),
            )
        }
    }
}

@Composable
private fun PremiumStatusPill(
    statusLabel: String,
    isActive: Boolean,
    isPending: Boolean,
) {
    val color = when {
        isActive -> MaterialTheme.colorScheme.primaryContainer
        isPending -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isActive -> MaterialTheme.colorScheme.onPrimaryContainer
        isPending -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        color = color,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}
