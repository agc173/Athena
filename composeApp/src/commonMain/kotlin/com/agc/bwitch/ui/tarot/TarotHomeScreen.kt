package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.economy.runWithEconomyGate
import com.agc.bwitch.presentation.tarot.TarotViewModel
import com.agc.bwitch.ui.common.economy.resolveEconomyGateLabel
import org.koin.compose.koinInject

@Composable
fun TarotHomeScreen(
    contentPadding: PaddingValues,
    onSelectRequestType: (TarotRequestType) -> Unit,
    viewModel: TarotViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
) {
    val strings = appStrings.tarot
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val tarot1CostLabel = resolveEconomyGateLabel(
        preview = economyState.modulePreviews.firstOrNull { it.module == "TAROT_1" },
        economyStrings = appStrings.economy,
        fallbackCost = 1,
        freeLabelOverride = appStrings.economy.freeToday,
    )
    val tarot3CostLabel = resolveEconomyGateLabel(
        preview = economyState.modulePreviews.firstOrNull { it.module == "TAROT_3" },
        economyStrings = appStrings.economy,
        fallbackCost = 3,
        freeLabelOverride = appStrings.economy.freeThisWeek,
    )
    val tarot1Preview = economyState.modulePreviews.firstOrNull { it.module == "TAROT_1" }
    val tarot3Preview = economyState.modulePreviews.firstOrNull { it.module == "TAROT_3" }
    val hasPendingSession = state.hasActiveRecoverableSession

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            strings.homeIntro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (hasPendingSession) {
            TarotOptionCard(
                title = strings.pendingSessionTitle,
                subtitle = strings.pendingSessionBody,
                costLabel = null,
                onClick = { onSelectRequestType(state.selectedType) },
            )
        }

        TarotOptionCard(
            title = strings.homeSingleCardTitle,
            subtitle = strings.homeSingleCardSubtitle,
            costLabel = tarot1CostLabel,
            onClick = {
                if (hasPendingSession) {
                    onSelectRequestType(TarotRequestType.TAROT_1)
                    return@TarotOptionCard
                }
                handleTarotSelection(
                    type = TarotRequestType.TAROT_1,
                    preview = tarot1Preview,
                    economyViewModel = economyViewModel,
                    onSelectRequestType = onSelectRequestType,
                )
            },
        )

        TarotOptionCard(
            title = strings.homeThreeCardTitle,
            subtitle = strings.homeThreeCardSubtitle,
            costLabel = tarot3CostLabel,
            onClick = {
                if (hasPendingSession) {
                    onSelectRequestType(TarotRequestType.TAROT_3)
                    return@TarotOptionCard
                }
                handleTarotSelection(
                    type = TarotRequestType.TAROT_3,
                    preview = tarot3Preview,
                    economyViewModel = economyViewModel,
                    onSelectRequestType = onSelectRequestType,
                )
            },
        )

    }
}


private fun handleTarotSelection(
    type: TarotRequestType,
    preview: EconomyModulePreview?,
    economyViewModel: EconomyViewModel,
    onSelectRequestType: (TarotRequestType) -> Unit,
) {
    runWithEconomyGate(
        preview = preview,
        economyViewModel = economyViewModel,
        source = when (type) {
            TarotRequestType.TAROT_1 -> "tarot_single_card"
            TarotRequestType.TAROT_3 -> "tarot_extra_reading"
        },
        fallbackCost = when (type) {
            TarotRequestType.TAROT_1 -> 1
            TarotRequestType.TAROT_3 -> 3
        },
    ) {
        onSelectRequestType(type)
    }
}

@Composable
private fun TarotOptionCard(
    title: String,
    subtitle: String,
    costLabel: String?,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            costLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
