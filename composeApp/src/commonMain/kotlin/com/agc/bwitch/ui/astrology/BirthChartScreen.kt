package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartUiState
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_ERROR_FALLBACK_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_FIRST_ERROR_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_UNAVAILABLE_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_REFRESH_ERROR_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_SAVE_ERROR_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_SAVE_SUCCESS_SUMMARY_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_SYNC_NO_ESSENCE_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_SYNC_REMOTE_LOADED_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_SYNC_UPDATED_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_SYNC_UP_TO_DATE_KEY
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.economy.runWithEconomyGate
import com.agc.bwitch.presentation.economy.toModuleCostUiStateOrNull
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.common.localization.resolve
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun BirthChartScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: BirthChartViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val strings = appStrings
    val birthChartStrings = strings.birthChart
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val birthEssencePreview = economyState.modulePreviews.firstOrNull {
        it.module == "BIRTH_ESSENCE" || it.module == "NATAL_ESSENCE"
    }
    val birthEssenceCostState = birthEssencePreview?.toModuleCostUiStateOrNull()
    val shareLauncher = rememberBirthEssenceShareLauncher(birthChartStrings)
    var shareError by remember { mutableStateOf<String?>(null) }
    var sharePreviewEssence by remember { mutableStateOf<BirthEssenceProfile?>(null) }
    var wasGenerating by remember { mutableStateOf(false) }

    LaunchedEffect(state.isGenerating, state.hasGeneratedResult, state.error) {
        if (wasGenerating && !state.isGenerating) {
            val endedWithResult = state.hasGeneratedResult
            val endedWithEconomyError = state.error.isBirthEssenceEconomyError()
            if (endedWithResult || endedWithEconomyError) {
                economyViewModel.loadEconomy()
            }
        }
        wasGenerating = state.isGenerating
    }

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        Text(
            birthChartStrings.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            birthChartStrings.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary,
        )

        SignDropdown(
            label = birthChartStrings.sunSignLabel,
            selected = state.selectedSunSign,
            strings = strings,
            enabled = !state.isBusy,
            onSelect = viewModel::onSunSignChange,
        )

        SignDropdown(
            label = birthChartStrings.moonSignLabel,
            selected = state.selectedMoonSign,
            strings = strings,
            enabled = !state.isBusy,
            onSelect = viewModel::onMoonSignChange,
        )

        SignDropdown(
            label = birthChartStrings.risingSignLabel,
            selected = state.selectedRisingSign,
            strings = strings,
            enabled = !state.isBusy,
            onSelect = viewModel::onRisingSignChange,
        )

        birthEssenceCostState?.let { costState ->
            Text(
                text = costState.label.resolve(appStrings.economy),
                style = MaterialTheme.typography.bodySmall,
                color = extras.textSecondary,
            )
        }

        BWitchPrimaryButton(
            onClick = {
                runWithEconomyGate(
                    preview = birthEssencePreview,
                    economyViewModel = economyViewModel,
                    source = "birth_essence",
                ) {
                    viewModel.discoverEssence()
                }
            },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isGenerating) birthChartStrings.discoverLoading else birthChartStrings.discoverCta)
        }

        if (state.hasGeneratedResult) {
            BWitchCard {
                Text(birthChartStrings.triadTitle, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${birthChartStrings.triadSunLabel}: ${(state.generatedSunSign ?: state.selectedSunSign).toDisplayName(strings)} · " +
                        "${birthChartStrings.triadMoonLabel}: ${(state.generatedMoonSign ?: state.selectedMoonSign).toDisplayName(strings)} · " +
                        "${birthChartStrings.triadAscLabel}: ${(state.generatedRisingSign ?: state.selectedRisingSign).toDisplayName(strings)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
                state.generatedArchetype?.let {
                    Text(birthChartStrings.archetypeLabel, style = MaterialTheme.typography.labelLarge, color = extras.textSecondary)
                    Text(it.displayName(state.currentLanguageCode), style = MaterialTheme.typography.titleLarge)
                    ArchetypeVisual(
                        archetype = it,
                        languageCode = state.currentLanguageCode,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                    )
                }
                Text(
                    text = state.generatedInterpretation.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                BWitchPrimaryButton(
                    onClick = viewModel::saveActiveEssence,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSaving) birthChartStrings.saveLoading else birthChartStrings.saveCta)
                }
                BWitchPrimaryButton(
                    onClick = {
                        shareError = null
                        val shareProfile = state.toShareProfileOrNull()
                        if (shareProfile == null) {
                            shareError = birthChartStrings.shareValidationError
                            return@BWitchPrimaryButton
                        }
                        sharePreviewEssence = shareProfile
                    },
                    enabled = !state.isBusy && state.hasGeneratedResult,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(birthChartStrings.shareCta)
                }
            }
        }

        BWitchPrimaryButton(
            onClick = viewModel::refresh,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isRefreshing) birthChartStrings.syncLoading else birthChartStrings.syncCta)
        }

        state.error?.let { Text(it.toBirthChartUiText(birthChartStrings), color = MaterialTheme.colorScheme.error) }
        shareError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedSummary?.let { Text(it.toBirthChartUiText(birthChartStrings), color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }

    sharePreviewEssence?.let { essence ->
        ShareEssencePreviewDialog(
            essence = essence,
            onDismiss = { sharePreviewEssence = null },
            onShareNow = { captureBounds ->
                shareLauncher
                    .share(essence = essence, captureBounds = captureBounds)
                    .onSuccess { sharePreviewEssence = null }
                    .onFailure { shareError = it.message ?: birthChartStrings.shareFailedFallback }
            }
        )
    }
}

private fun String?.isBirthEssenceEconomyError(): Boolean {
    val normalized = this?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return false
    return normalized.contains("insufficient_moons")
}

private fun String.toBirthChartUiText(strings: com.agc.bwitch.localization.BirthChartStrings): String {
    return when (this) {
        BIRTH_CHART_SYNC_NO_ESSENCE_KEY -> strings.syncNoEssenceSummary
        BIRTH_CHART_SYNC_REMOTE_LOADED_KEY -> strings.syncRemoteLoadedSummary
        BIRTH_CHART_SYNC_UPDATED_KEY -> strings.syncUpdatedSummary
        BIRTH_CHART_SYNC_UP_TO_DATE_KEY -> strings.syncAlreadyUpToDateSummary
        BIRTH_CHART_REFRESH_ERROR_KEY -> strings.refreshErrorMessage
        BIRTH_CHART_GENERATE_FIRST_ERROR_KEY -> strings.generateFirstError
        BIRTH_CHART_SAVE_SUCCESS_SUMMARY_KEY -> strings.saveSuccessSummary
        BIRTH_CHART_SAVE_ERROR_KEY -> strings.saveErrorMessage
        BIRTH_CHART_GENERATE_UNAVAILABLE_KEY -> strings.generateUnavailableError
        BIRTH_CHART_GENERATE_ERROR_FALLBACK_KEY -> strings.generateErrorFallback
        else -> strings.generateErrorFallback
    }
}

private fun BirthChartUiState.toShareProfileOrNull(): BirthEssenceProfile? {
    val interpretation = generatedInterpretation?.trim().orEmpty()
    if (interpretation.isBlank()) return null

    return BirthEssenceProfile(
        sunSign = generatedSunSign ?: selectedSunSign,
        moonSign = generatedMoonSign ?: selectedMoonSign,
        risingSign = generatedRisingSign ?: selectedRisingSign,
        interpretation = interpretation,
        languageCode = generatedLanguageCode,
        archetype = generatedArchetype,
        savedAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )
}

@Composable
private fun SignDropdown(
    label: String,
    selected: ZodiacSign,
    strings: AppStrings,
    enabled: Boolean,
    onSelect: (ZodiacSign) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected.toDisplayName(strings),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (expanded) {
            SignPickerDialog(
                title = label,
                selected = selected,
                strings = strings,
                onSelect = {
                    onSelect(it)
                    expanded = false
                },
                onDismiss = { expanded = false }
            )
        }
    }
}

@Composable
private fun SignPickerDialog(
    title: String,
    selected: ZodiacSign,
    strings: AppStrings,
    onSelect: (ZodiacSign) -> Unit,
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
                    .padding(spacing.spacingMd),
                verticalArrangement = Arrangement.spacedBy(spacing.spacingXs)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacingXs)
                ) {
                    ZodiacSign.entries.forEach { sign ->
                        OutlinedButton(
                            onClick = { onSelect(sign) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val signText = sign.toDisplayName(strings)
                            val text = if (sign == selected) "$signText ✓" else signText
                            Text(text)
                        }
                    }
                }
            }
        }
    }
}

private fun ZodiacSign.toDisplayName(strings: AppStrings): String = when (this) {
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

@Composable
private fun ShareEssencePreviewDialog(
    essence: BirthEssenceProfile,
    onDismiss: () -> Unit,
    onShareNow: (ShareCaptureBounds) -> Unit,
) {
    val spacing = BWitchThemeTokens.dimens
    val birthChartStrings = appStrings.birthChart
    var captureBounds by remember { mutableStateOf<ShareCaptureBounds?>(null) }

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
                    .padding(spacing.spacingMd),
                verticalArrangement = Arrangement.spacedBy(spacing.spacingSm)
            ) {
                Text(
                    text = birthChartStrings.sharePreviewTitle,
                    style = MaterialTheme.typography.titleMedium
                )
                BirthEssenceShareCard(
                    essence = essence,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            val rect = coordinates.boundsInWindow()
                            captureBounds = ShareCaptureBounds(
                                left = rect.left.toInt(),
                                top = rect.top.toInt(),
                                width = rect.width.toInt(),
                                height = rect.height.toInt(),
                            )
                        },
                )
                BWitchPrimaryButton(
                    onClick = {
                        captureBounds?.let(onShareNow)
                    },
                    enabled = captureBounds != null,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(birthChartStrings.shareNowCta)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(birthChartStrings.shareCancelCta)
                }
            }
        }
    }
}

@Composable
private fun ArchetypeVisual(
    archetype: BirthEssenceArchetype,
    languageCode: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(archetype.toVisualResource()),
            contentDescription = "${appStrings.birthChart.archetypeVisualContentDescriptionPrefix} ${archetype.displayName(languageCode)}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
            contentScale = ContentScale.Fit,
        )
    }
}
