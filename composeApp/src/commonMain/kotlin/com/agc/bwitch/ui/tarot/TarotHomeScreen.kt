package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.theme.BWitchThemeTokens
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
    onSelectLastReading: () -> Unit,
    viewModel: TarotViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
) {
    val strings = appStrings.tarot
    val economyState by economyViewModel.uiState.collectAsState()
    val tarot1Preview = economyState.modulePreviews.firstOrNull { it.module == "TAROT_1" }
    val tarot3Preview = economyState.modulePreviews.firstOrNull { it.module == "TAROT_3" }
    val tarot1CostLabel = resolveEconomyGateLabel(
        preview = tarot1Preview,
        economyStrings = appStrings.economy,
        fallbackCost = 1,
        freeLabelOverride = appStrings.economy.freeToday,
    ) ?: appStrings.economy.moonCostFormat.replaceFirst("%d", "1")
    val tarot3CostLabel = resolveEconomyGateLabel(
        preview = tarot3Preview,
        economyStrings = appStrings.economy,
        fallbackCost = 3,
        freeLabelOverride = appStrings.economy.freeThisWeek,
    ) ?: appStrings.economy.moonCostFormat.replaceFirst("%d", "3")
    val hasSavedReading = viewModel.hasSavedReading()

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

        TarotOptionCard(
            title = strings.homeSingleCardTitle,
            subtitle = strings.homeSingleCardSubtitle,
            costLabel = tarot1CostLabel,
            onClick = {
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
                handleTarotSelection(
                    type = TarotRequestType.TAROT_3,
                    preview = tarot3Preview,
                    economyViewModel = economyViewModel,
                    onSelectRequestType = onSelectRequestType,
                )
            },
        )
        if (hasSavedReading) {
            TarotOptionCard(
                title = strings.homeLastReadingTitle,
                subtitle = strings.homeLastReadingSubtitle,
                costLabel = null,
                onClick = onSelectLastReading,
            )
        }

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
    val dimens = BWitchThemeTokens.dimens

    BWitchCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp),
        onClick = onClick,
        contentPadding = PaddingValues(dimens.spacingMd),
        contentVerticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (costLabel != null) {
            Text(
                text = costLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
