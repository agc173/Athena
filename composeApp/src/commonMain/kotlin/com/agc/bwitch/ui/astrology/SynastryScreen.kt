package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryDailyAxisState
import com.agc.bwitch.domain.astrology.synastry.SynastryDimension
import com.agc.bwitch.domain.astrology.synastry.SynastryEnergyAxis
import com.agc.bwitch.domain.astrology.synastry.SynastryReading
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingDepth
import com.agc.bwitch.domain.astrology.synastry.primaryGuidanceCopy
import com.agc.bwitch.domain.astrology.synastry.primaryStrengthCopy
import com.agc.bwitch.domain.astrology.synastry.primaryTensionCopy
import com.agc.bwitch.domain.astrology.synastry.toFiveStarRating
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.platform.share.ShareResult
import com.agc.bwitch.platform.share.ShareTextPayload
import com.agc.bwitch.platform.share.rememberShareLauncher
import com.agc.bwitch.presentation.astrology.synastry.SynastryPersonForm
import com.agc.bwitch.presentation.astrology.synastry.SynastryViewModel
import com.agc.bwitch.presentation.astrology.synastry.SynastryUiEffect
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.economy.runWithEconomyGate
import com.agc.bwitch.ui.common.economy.DailyLimitPaywallCard
import com.agc.bwitch.ui.common.economy.EconomyGateInfoRow
import com.agc.bwitch.ui.common.economy.isDailyLimitRejected
import com.agc.bwitch.ui.common.economy.hasPremiumBenefit
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import com.agc.bwitch.ui.tarot.DeckCardUnlockRewardDialog
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SynastryScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SynastryViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    onOpenStore: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val synastryPreview = economyState.modulePreviews.firstOrNull { it.module == "SYNASTRY" }
    val spacing = BWitchThemeTokens.dimens
    val strings = appStrings
    val synastryStrings = strings.synastry
    var wasGenerating by remember { mutableStateOf(false) }
    val shareLauncher = rememberShareLauncher()
    val shareScope = rememberCoroutineScope()
    var shareErrorMessage by remember { mutableStateOf<String?>(null) }
    var rewardDialogRewards by remember { mutableStateOf<List<DeckCardUnlockReward>>(emptyList()) }
    val showDailyLimitPaywall = synastryPreview.isDailyLimitRejected() || state.error == "daily_limit"

    LaunchedEffect(state.isGenerating, state.reading, state.error) {
        if (wasGenerating && !state.isGenerating) {
            val endedWithReading = state.reading != null
            val endedWithEconomyError = state.error == "insufficient_moons" || state.error == "daily_limit"
            if (endedWithReading || endedWithEconomyError) {
                economyViewModel.loadEconomy()
            }
        }
        wasGenerating = state.isGenerating
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is SynastryUiEffect.ShowDeckCardUnlockRewards -> rewardDialogRewards = effect.rewards
            }
        }
    }

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(spacing.spacingMd)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.spacingSm + spacing.spacingXs)
    ) {
        Text(
            text = synastryStrings.screenTitle,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = synastryStrings.screenSubtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = synastryStrings.screenSupportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PersonFormCard(
            personA = state.personA,
            personB = state.personB,
            onPersonASunChange = viewModel::onPersonASunSignChange,
            onPersonAMoonChange = viewModel::onPersonAMoonSignChange,
            onPersonARisingChange = viewModel::onPersonARisingSignChange,
            onPersonBSunChange = viewModel::onPersonBSunSignChange,
            onPersonBMoonChange = viewModel::onPersonBMoonSignChange,
            onPersonBRisingChange = viewModel::onPersonBRisingSignChange,
            strings = strings,
        )

        EconomyGateInfoRow(
            preview = synastryPreview,
            economyStrings = strings.economy,
            fallbackCost = 1,
            packUsesLabel = strings.economy.synastryPackValueFormat,
        )
        if (showDailyLimitPaywall) {
            DailyLimitPaywallCard(
                economyStrings = strings.economy,
                onOpenStore = onOpenStore,
                module = synastryPreview?.module ?: "SYNASTRY",
                placement = "synastry_daily_limit",
                reason = synastryPreview?.reasonIfRejected ?: "daily_limit",
                hasPremiumBenefit = synastryPreview.hasPremiumBenefit(),
                onPaywallShown = economyViewModel::onDailyLimitPaywallShown,
                onPaywallActionClicked = economyViewModel::onDailyLimitPaywallActionClicked,
            )
        }
        BWitchPrimaryButton(
            onClick = {
                runWithEconomyGate(
                    preview = synastryPreview,
                    economyViewModel = economyViewModel,
                    source = "synastry",
                    fallbackCost = (synastryPreview?.cost ?: 0).takeIf { it > 0 } ?: 1,
                ) { viewModel.generate() }
            },
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = if (state.isGenerating) synastryStrings.calculatingCta else synastryStrings.calculateCta,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }

        state.error?.takeUnless { it == "daily_limit" }?.let { error ->
            Text(
                text = when (error) {
                    "required_sun_signs_error" -> synastryStrings.requiredSunSignsError
                    "insufficient_moons" -> strings.economy.notEnoughMoons
                    "generic_generate_error" -> synastryStrings.genericGenerateError
                    else -> synastryStrings.genericGenerateError
                },
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.reading?.let { reading ->
            SynastryResultCard(
                reading = reading,
                strings = strings,
                languageCode = state.currentLanguageCode,
                onShare = {
                    shareErrorMessage = null
                    shareScope.launch {
                        val shareResult = shareLauncher.shareText(
                            ShareTextPayload(text = it, title = strings.horoscope.shareCta),
                        )
                        if (shareResult is ShareResult.Error) {
                            shareErrorMessage = shareResult.message ?: strings.birthChart.shareFailedFallback
                        }
                    }
                },
            )
        }
        shareErrorMessage?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    }
    if (rewardDialogRewards.isNotEmpty()) {
        DeckCardUnlockRewardDialog(
            strings = appStrings,
            rewards = rewardDialogRewards,
            onDismiss = { rewardDialogRewards = emptyList() },
            onOpenCollection = {
                rewardDialogRewards = emptyList()
                onOpenCollection()
            },
        )
    }
}

@Composable
private fun PersonFormCard(
    personA: SynastryPersonForm,
    personB: SynastryPersonForm,
    onPersonASunChange: (ZodiacSign?) -> Unit,
    onPersonAMoonChange: (ZodiacSign?) -> Unit,
    onPersonARisingChange: (ZodiacSign?) -> Unit,
    onPersonBSunChange: (ZodiacSign?) -> Unit,
    onPersonBMoonChange: (ZodiacSign?) -> Unit,
    onPersonBRisingChange: (ZodiacSign?) -> Unit,
    strings: AppStrings,
) {
    val synastryStrings = strings.synastry
    BWitchCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = synastryStrings.cardALabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "↔",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = synastryStrings.cardBLabel,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }

        ComparativeSignRow(
            label = synastryStrings.sunSignLabel,
            aSelected = personA.sunSign,
            bSelected = personB.sunSign,
            allowEmpty = false,
            onSelectA = onPersonASunChange,
            onSelectB = onPersonBSunChange,
            strings = strings,
        )

        ComparativeSignRow(
            label = synastryStrings.moonLabel,
            aSelected = personA.moonSign,
            bSelected = personB.moonSign,
            allowEmpty = true,
            onSelectA = onPersonAMoonChange,
            onSelectB = onPersonBMoonChange,
            strings = strings,
        )

        ComparativeSignRow(
            label = synastryStrings.risingLabel,
            aSelected = personA.risingSign,
            bSelected = personB.risingSign,
            allowEmpty = true,
            onSelectA = onPersonARisingChange,
            onSelectB = onPersonBRisingChange,
            strings = strings,
        )
    }
}

@Composable
private fun ComparativeSignRow(
    label: String,
    aSelected: ZodiacSign?,
    bSelected: ZodiacSign?,
    allowEmpty: Boolean,
    onSelectA: (ZodiacSign?) -> Unit,
    onSelectB: (ZodiacSign?) -> Unit,
    strings: AppStrings,
) {
    val synastryStrings = strings.synastry
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignDropdown(
                label = synastryStrings.cardALabel,
                selected = aSelected,
                allowEmpty = allowEmpty,
                onSelect = onSelectA,
                showLabel = false,
                modifier = Modifier.weight(1f),
                strings = strings,
            )
            SignDropdown(
                label = synastryStrings.cardBLabel,
                selected = bSelected,
                allowEmpty = allowEmpty,
                onSelect = onSelectB,
                showLabel = false,
                modifier = Modifier.weight(1f),
                strings = strings,
            )
        }
    }
}

@Composable
private fun SynastryResultCard(
    reading: SynastryReading,
    strings: AppStrings,
    languageCode: String,
    onShare: (String) -> Unit,
) {
    val structured = reading.structured
    val synastryStrings = strings.synastry

    BWitchCard {
        Text(
            text = synastryStrings.resultTitle,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = structured.depthInfo.depth.toUiDepthLabel(strings),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        SectionSeparator()
        ResultBlockTitle(synastryStrings.bondMetricsTitle)
        metricOrder.forEach { dimension ->
            val score = structured.scores[dimension] ?: return@forEach
            MetricStarsRow(dimension = dimension, stars = score.toFiveStarRating(), strings = strings)
        }

        reading.dailyOverlay?.let { daily ->
            SectionSeparator()
            ResultBlockTitle(synastryStrings.dailyEnergyTitle)
            daily.axes.forEach { axis ->
                DailyEnergyAxisRow(axis, strings)
            }
        }

        SectionSeparator()
        ResultBlockTitle(synastryStrings.strengthTitle)
        Text(
            text = reading.primaryStrengthCopy(languageCode),
            style = MaterialTheme.typography.bodyLarge,
        )

        ResultBlockTitle(synastryStrings.tensionTitle)
        Text(
            text = reading.primaryTensionCopy(languageCode),
            style = MaterialTheme.typography.bodyLarge,
        )

        ResultBlockTitle(synastryStrings.guidanceTitle)
        Text(
            text = reading.primaryGuidanceCopy(languageCode),
            style = MaterialTheme.typography.bodyLarge,
        )

        reading.dailyOverlay?.let { daily ->
            ResultBlockTitle(synastryStrings.dailyGuidanceTitle)
            Text(
                text = daily.dailyGuidance,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionSeparator()
        ResultBlockTitle(synastryStrings.narrativeTitle)
        Text(
            text = reading.narrative,
            style = MaterialTheme.typography.bodyLarge,
        )

        val shareText = buildSynastryShareText(reading = reading, strings = strings, languageCode = languageCode)
        if (shareText.isNotBlank()) {
            SectionSeparator()
            OutlinedButton(
                onClick = { onShare(shareText) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(strings.horoscope.shareCta) }
        }
    }
}

private fun buildSynastryShareText(reading: SynastryReading, strings: AppStrings, languageCode: String): String {
    val synastry = strings.synastry
    val sections = listOf(
        synastry.resultTitle,
        reading.narrative,
        "${synastry.strengthTitle}\n${reading.primaryStrengthCopy(languageCode)}",
        "${synastry.tensionTitle}\n${reading.primaryTensionCopy(languageCode)}",
        "${synastry.guidanceTitle}\n${reading.primaryGuidanceCopy(languageCode)}",
    )

    return sections
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .joinToString("\n\n")
}

@Composable
private fun MetricStarsRow(dimension: SynastryDimension, stars: Double, strings: AppStrings) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = dimension.toUiLabel(strings),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
        Spacer(modifier = Modifier.width(12.dp))
        StarRating(stars = stars)
    }
}

@Composable
private fun StarRating(stars: Double) {
    val normalized = stars.coerceIn(0.0, 5.0)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            val fillFraction = (normalized - index).coerceIn(0.0, 1.0).toFloat()
            StarCell(fillFraction = fillFraction)
        }
    }
}

@Composable
private fun StarCell(fillFraction: Float) {
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(16.dp)
            .drawWithContent {
                val starPath = buildStarPath(size.width, size.height)
                drawPath(path = starPath, color = inactiveColor, style = Fill)
                if (fillFraction > 0f) {
                    clipRect(left = 0f, top = 0f, right = size.width * fillFraction, bottom = size.height) {
                        drawPath(path = starPath, color = activeColor, style = Fill)
                    }
                }
            }
    )
}

@Composable
private fun DailyEnergyAxisRow(axis: SynastryDailyAxisState, strings: AppStrings) {
    val position = ((axis.value + 100f) / 200f).coerceIn(0f, 1f)
    val markerSize = 20.dp
    var showAxisInfo by remember(axis.axis) { mutableStateOf(false) }

    val moonColor = MaterialTheme.colorScheme.tertiary
    val moonCutoutColor = MaterialTheme.colorScheme.surface
    val markerBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val axisLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val axisLeftColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
    val axisCenterColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    val axisRightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isCompactHeader = maxWidth < 320.dp
            val showDecorativeDots = maxWidth >= 280.dp
            val labelStyle = if (maxWidth < 280.dp) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            }.copy(fontWeight = FontWeight.Medium)
            val horizontalGap = if (isCompactHeader) 6.dp else 8.dp
            val sidePadding = if (isCompactHeader) 0.dp else 4.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(horizontalGap),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = sidePadding),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = axis.leftLabel(strings),
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(horizontalGap),
                ) {
                    if (showDecorativeDots) {
                        Text(
                            text = "• •",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    AxisInfoButton(onClick = { showAxisInfo = true })
                    if (showDecorativeDots) {
                        Text(
                            text = "• •",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = sidePadding),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Text(
                        text = axis.rightLabel(strings),
                        style = labelStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        softWrap = false,
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
        ) {
            val travelRange = (maxWidth - markerSize).coerceAtLeast(0.dp)
            val markerOffset = travelRange * position

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                axisLeftColor,
                                axisCenterColor,
                                axisRightColor,
                            )
                        )
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(16.dp)
                    .background(axisLineColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = markerOffset)
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(moonCutoutColor)
                    .border(1.5.dp, markerBorderColor, CircleShape),
            )
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = markerOffset + 2.dp, y = 2.dp)
                    .size(markerSize - 4.dp),
            ) {
                drawCircle(color = moonColor)
                drawCircle(
                    color = moonCutoutColor,
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.64f, size.height * 0.5f),
                )
            }
        }

        if (showAxisInfo) {
            AxisInfoDialog(axis = axis.axis, strings = strings, onDismiss = { showAxisInfo = false })
        }
    }
}

@Composable
private fun AxisInfoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "i",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SectionSeparator() {
    Text(
        text = "✦  ✦  ✦",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ResultBlockTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AxisInfoDialog(
    axis: SynastryEnergyAxis,
    strings: AppStrings,
    onDismiss: () -> Unit,
) {
    val synastryStrings = strings.synastry
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp)),
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = axis.axisTitle(strings),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = axis.axisInfoDescription(strings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(synastryStrings.closeCta)
                }
            }
        }
    }
}

private fun buildStarPath(width: Float, height: Float): Path {
    val center = Offset(width / 2f, height / 2f)
    val outerRadius = minOf(width, height) / 2f
    val innerRadius = outerRadius * 0.5f
    val path = Path()

    repeat(10) { index ->
        val isOuter = index % 2 == 0
        val radius = if (isOuter) outerRadius.toDouble() else innerRadius.toDouble()
        val angle = (-90.0 + index * 36.0) * (kotlin.math.PI / 180.0)
        val x = center.x + (kotlin.math.cos(angle) * radius).toFloat()
        val y = center.y + (kotlin.math.sin(angle) * radius).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private fun SynastryEnergyAxis.axisTitle(strings: AppStrings): String = when (this) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> strings.synastry.axisHarmonyIntensityTitle
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> strings.synastry.axisStabilityTransformationTitle
    SynastryEnergyAxis.CALM_MOVEMENT -> strings.synastry.axisCalmMovementTitle
}

private fun SynastryEnergyAxis.axisInfoDescription(strings: AppStrings): String = when (this) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> strings.synastry.axisHarmonyIntensityDescription
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> strings.synastry.axisStabilityTransformationDescription
    SynastryEnergyAxis.CALM_MOVEMENT -> strings.synastry.axisCalmMovementDescription
}

private val metricOrder = listOf(
    SynastryDimension.ATTRACTION,
    SynastryDimension.EMOTIONAL,
    SynastryDimension.COMMUNICATION,
    SynastryDimension.GROWTH,
)

@Composable
private fun SignDropdown(
    label: String,
    selected: ZodiacSign?,
    allowEmpty: Boolean,
    onSelect: (ZodiacSign?) -> Unit,
    strings: AppStrings,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs)
    ) {
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected?.localizedLabel(strings) ?: strings.synastry.selectPlaceholder,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
            )
        }

        if (expanded) {
            SignPickerDialog(
                title = label,
                selected = selected,
                allowEmpty = allowEmpty,
                strings = strings,
                onSelect = {
                    onSelect(it)
                    expanded = false
                },
                onDismiss = { expanded = false },
            )
        }
    }
}

@Composable
private fun SignPickerDialog(
    title: String,
    selected: ZodiacSign?,
    allowEmpty: Boolean,
    strings: AppStrings,
    onSelect: (ZodiacSign?) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = BWitchThemeTokens.dimens

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = spacing.spacingXs,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacingMd)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.spacingXs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                )

                if (allowEmpty) {
                    OutlinedButton(
                        onClick = { onSelect(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (selected == null) "${strings.synastry.unspecifiedOption} ✓" else strings.synastry.unspecifiedOption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    }
                }

                ZodiacSign.entries.forEach { sign ->
                    OutlinedButton(
                        onClick = { onSelect(sign) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (sign == selected) "${sign.localizedLabel(strings)} ✓" else sign.localizedLabel(strings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            softWrap = false,
                        )
                    }
                }
            }
        }
    }
}

private fun ZodiacSign.localizedLabel(strings: AppStrings): String = when (this) {
    ZodiacSign.aries -> strings.zodiac.aries
    ZodiacSign.taurus -> strings.zodiac.taurus
    ZodiacSign.gemini -> strings.zodiac.gemini
    ZodiacSign.cancer -> strings.zodiac.cancer
    ZodiacSign.leo -> strings.zodiac.leo
    ZodiacSign.virgo -> strings.zodiac.virgo
    ZodiacSign.libra -> strings.zodiac.libra
    ZodiacSign.scorpio -> strings.zodiac.scorpio
    ZodiacSign.sagittarius -> strings.zodiac.sagittarius
    ZodiacSign.capricorn -> strings.zodiac.capricorn
    ZodiacSign.aquarius -> strings.zodiac.aquarius
    ZodiacSign.pisces -> strings.zodiac.pisces
}

private fun SynastryReadingDepth.toUiDepthLabel(strings: AppStrings): String = when (this) {
    SynastryReadingDepth.BASIC -> strings.synastry.depthBasic
    SynastryReadingDepth.PARTIAL -> strings.synastry.depthPartial
    SynastryReadingDepth.COMPLETE -> strings.synastry.depthComplete
}

private fun SynastryDimension.toUiLabel(strings: AppStrings): String = when (this) {
    SynastryDimension.ATTRACTION -> strings.synastry.dimensionAttraction
    SynastryDimension.EMOTIONAL -> strings.synastry.dimensionEmotional
    SynastryDimension.COMMUNICATION -> strings.synastry.dimensionCommunication
    SynastryDimension.GROWTH -> strings.synastry.dimensionGrowth
}

private fun SynastryDailyAxisState.leftLabel(strings: AppStrings): String = when (axis) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> strings.synastry.axisHarmony
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> strings.synastry.axisStability
    SynastryEnergyAxis.CALM_MOVEMENT -> strings.synastry.axisCalm
}

private fun SynastryDailyAxisState.rightLabel(strings: AppStrings): String = when (axis) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> strings.synastry.axisIntensity
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> strings.synastry.axisTransformation
    SynastryEnergyAxis.CALM_MOVEMENT -> strings.synastry.axisMovement
}
