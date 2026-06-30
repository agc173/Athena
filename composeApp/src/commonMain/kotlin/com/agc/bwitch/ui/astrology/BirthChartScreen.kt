package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.natal.BirthDateTimeLocal
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.BirthplacePreset
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.ZodiacSign as NatalZodiacSign
import com.agc.bwitch.domain.astrology.natal.toUtc
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.BirthChartStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartUiState
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartUiEffect
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_CONNECTION_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_DAILY_LIMIT_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_ERROR_FALLBACK_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_FIRST_ERROR_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_INSUFFICIENT_MOONS_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_SESSION_EXPIRED_KEY
import com.agc.bwitch.presentation.astrology.birthchart.BIRTH_CHART_GENERATE_TEMPORARY_ATHENA_KEY
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
import com.agc.bwitch.ui.common.BirthDateSelector
import com.agc.bwitch.ui.common.BirthTimeSelector
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.common.economy.DailyLimitPaywallCard
import com.agc.bwitch.ui.common.economy.EconomyGateInfoRow
import com.agc.bwitch.ui.astrology.birthplace.BirthplacePresets
import com.agc.bwitch.ui.astrology.birthplace.DefaultBirthplaceCatalogRepository
import com.agc.bwitch.ui.astrology.birthplace.matchesBirthplaceQuery
import com.agc.bwitch.ui.astrology.birthplace.rankBirthplaceMatches
import com.agc.bwitch.ui.common.economy.isDailyLimitRejected
import com.agc.bwitch.ui.common.economy.hasPremiumBenefit
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import com.agc.bwitch.ui.tarot.DeckCardUnlockRewardDialog
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun BirthChartScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: BirthChartViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    onOpenStore: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val strings = appStrings
    val birthChartStrings = strings.birthChart
    val appName = strings.common.appName
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val economyState by economyViewModel.uiState.collectAsState()
    val birthEssencePreview = economyState.modulePreviews.firstOrNull {
        it.module == "BIRTH_ESSENCE" || it.module == "NATAL_ESSENCE"
    }
    val shareLauncher = rememberBirthEssenceShareLauncher(birthChartStrings)
    var shareError by remember { mutableStateOf<String?>(null) }
    var sharePreviewEssence by remember { mutableStateOf<BirthEssenceProfile?>(null) }
    var wasGenerating by remember { mutableStateOf(false) }
    var rewardDialogRewards by remember { mutableStateOf<List<DeckCardUnlockReward>>(emptyList()) }
    val showDailyLimitPaywall = birthEssencePreview.isDailyLimitRejected() || state.error.isDailyLimitError()

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
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is BirthChartUiEffect.ShowDeckCardUnlockRewards -> rewardDialogRewards = effect.rewards
            }
        }
    }

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        Text(
            birthChartStrings.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary,
        )

        BasicNatalChartSection(
            strings = birthChartStrings,
            appStrings = strings,
            economyViewModel = economyViewModel,
            onOpenStore = onOpenStore,
        )

        BWitchCard {
            Text(birthChartStrings.manualEssenceTitle, style = MaterialTheme.typography.titleLarge)

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

        EconomyGateInfoRow(
            preview = birthEssencePreview,
            economyStrings = appStrings.economy,
            fallbackCost = 5,
        )

        if (showDailyLimitPaywall) {
            DailyLimitPaywallCard(
                economyStrings = strings.economy,
                onOpenStore = onOpenStore,
                module = birthEssencePreview?.module ?: "BIRTH_ESSENCE",
                placement = "birth_essence_daily_limit",
                reason = birthEssencePreview?.reasonIfRejected ?: "daily_limit",
                hasPremiumBenefit = birthEssencePreview.hasPremiumBenefit(),
                onPaywallShown = economyViewModel::onDailyLimitPaywallShown,
                onPaywallActionClicked = economyViewModel::onDailyLimitPaywallActionClicked,
            )
        }

        BWitchPrimaryButton(
            onClick = {
                runWithEconomyGate(
                    preview = birthEssencePreview,
                    economyViewModel = economyViewModel,
                    source = "birth_essence",
                    fallbackCost = 5,
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

        state.error?.takeUnless { it.isDailyLimitError() }?.let { error ->
            Text(error.toBirthChartUiText(birthChartStrings), color = MaterialTheme.colorScheme.error)
        }
        shareError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedSummary?.let { Text(it.toBirthChartUiText(birthChartStrings), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }

    sharePreviewEssence?.let { essence ->
        ShareEssencePreviewDialog(
            essence = essence,
            appName = appName,
            onDismiss = { sharePreviewEssence = null },
            onShareNow = { captureBounds ->
                shareLauncher
                    .share(essence = essence, captureBounds = captureBounds)
                    .onSuccess { sharePreviewEssence = null }
                    .onFailure { shareError = it.message ?: birthChartStrings.shareFailedFallback }
            }
        )
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
private fun BasicNatalChartSection(
    strings: BirthChartStrings,
    appStrings: AppStrings,
    economyViewModel: EconomyViewModel,
    onOpenStore: () -> Unit,
    economyRepository: EconomyRepository = koinInject(),
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var birthHour by remember { mutableStateOf<Int?>(null) }
    var birthMinute by remember { mutableStateOf<Int?>(null) }
    var birthplaceQuery by remember { mutableStateOf("") }
    var selectedBirthplace by remember { mutableStateOf<BirthplacePreset?>(null) }
    var isBirthplaceDialogOpen by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<NatalChartResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasAttemptedBasicNatalCalculation by remember { mutableStateOf(false) }
    var isAuthorizingBasicNatal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val economyState by economyViewModel.uiState.collectAsState()
    val basicNatalPreview = economyState.modulePreviews.firstOrNull { it.module == "BASIC_NATAL_CHART" || it.module == "BASIC_NATAL" }
    val showBasicNatalDailyLimitPaywall = basicNatalPreview.isDailyLimitRejected()

    val birthplaceCatalogState by produceState(
        initialValue = BirthplaceCatalogUiState(
            presets = BirthplacePresets,
            isLoadingRuntimeCatalog = true,
        ),
    ) {
        value = BirthplaceCatalogUiState(
            presets = DefaultBirthplaceCatalogRepository.getBirthplaces(),
            isLoadingRuntimeCatalog = false,
        )
    }
    val matchingBirthplaces = remember(birthplaceQuery, birthplaceCatalogState.presets) {
        rankBirthplaceMatches(birthplaceQuery, birthplaceCatalogState.presets)
    }
    val validationMessage = remember(strings, birthDate, birthHour, birthMinute, selectedBirthplace) {
        validateBasicNatalChartInput(
            strings = strings,
            birthDate = birthDate,
            birthHour = birthHour,
            birthMinute = birthMinute,
            selectedBirthplace = selectedBirthplace,
        )
    }

    BWitchCard(contentVerticalArrangement = Arrangement.spacedBy(dimens.spacingMd)) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
            Text(strings.basicNatalTitle, style = MaterialTheme.typography.titleLarge)
            Text(
                strings.basicNatalSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = extras.textSecondary,
            )
        }

        BirthDateSelector(
            selectedDate = birthDate,
            onDateSelected = { birthDate = it },
            label = strings.basicNatalBirthDateLabel,
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
        )

        BirthTimeSelector(
            selectedHour = birthHour,
            selectedMinute = birthMinute,
            onTimeSelected = { hour, minute ->
                birthHour = hour
                birthMinute = minute
            },
            label = strings.basicNatalBirthTimeLabel,
            hourLabel = strings.basicNatalHourLabel,
            minuteLabel = strings.basicNatalMinuteLabel,
            pickerTitle = strings.basicNatalBirthTimeLabel,
            placeholder = strings.basicNatalBirthTimePlaceholder,
            enabled = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            Text(strings.basicNatalBirthplaceLabel, style = MaterialTheme.typography.labelLarge)
            Surface(
                onClick = { isBirthplaceDialogOpen = true },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Row(
                    modifier = Modifier.padding(dimens.spacingMd),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
                    ) {
                        Text(
                            text = selectedBirthplace?.displayName() ?: strings.basicNatalBirthplacePlaceholder,
                            style = if (selectedBirthplace == null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                            color = if (selectedBirthplace == null) extras.textSecondary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        text = if (selectedBirthplace == null) strings.basicNatalBirthplaceSearchLabel else strings.basicNatalBirthplaceChangeCta,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = strings.basicNatalBirthplaceSearchSupportingText,
                style = MaterialTheme.typography.bodySmall,
                color = extras.textSecondary,
            )
            if (birthplaceCatalogState.isLoadingRuntimeCatalog) {
                Text(
                    text = strings.basicNatalBirthplaceLoadingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.textSecondary,
                )
            }
        }

        if (isBirthplaceDialogOpen) {
            BirthplacePickerDialog(
                strings = strings,
                query = birthplaceQuery,
                matchingBirthplaces = matchingBirthplaces,
                isCatalogLoading = birthplaceCatalogState.isLoadingRuntimeCatalog,
                onQueryChange = { query ->
                    birthplaceQuery = query
                    if (selectedBirthplace != null && !selectedBirthplace!!.matchesBirthplaceQuery(query)) {
                        selectedBirthplace = null
                    }
                },
                onSelect = { preset ->
                    selectedBirthplace = preset
                    birthplaceQuery = preset.displayName()
                    isBirthplaceDialogOpen = false
                },
                onDismiss = { isBirthplaceDialogOpen = false },
            )
        }

        if (hasAttemptedBasicNatalCalculation) {
            validationMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }

        EconomyGateInfoRow(
            preview = basicNatalPreview,
            economyStrings = appStrings.economy,
            fallbackCost = 1,
            freeLabelOverride = strings.basicNatalFreeWeeklyLabel,
        )

        if (showBasicNatalDailyLimitPaywall) {
            DailyLimitPaywallCard(
                economyStrings = appStrings.economy,
                onOpenStore = onOpenStore,
                module = basicNatalPreview?.module ?: "BASIC_NATAL_CHART",
                placement = "basic_natal_daily_limit",
                reason = basicNatalPreview?.reasonIfRejected ?: "daily_limit",
                hasPremiumBenefit = basicNatalPreview.hasPremiumBenefit(),
                onPaywallShown = economyViewModel::onDailyLimitPaywallShown,
                onPaywallActionClicked = economyViewModel::onDailyLimitPaywallActionClicked,
            )
        }

        BWitchPrimaryButton(
            onClick = {
                hasAttemptedBasicNatalCalculation = true
                result = null
                error = null
                val message = validationMessage
                if (message != null) return@BWitchPrimaryButton
                val date = birthDate ?: return@BWitchPrimaryButton
                val hour = birthHour ?: return@BWitchPrimaryButton
                val minute = birthMinute ?: return@BWitchPrimaryButton
                val birthplace = selectedBirthplace ?: return@BWitchPrimaryButton
                val calculateAfterEconomyGate = {
                    coroutineScope.launch {
                        isAuthorizingBasicNatal = true
                        try {
                            // The local calculator runs only after economy authorization succeeds. If this
                            // exceptional local step fails afterwards, the reserved free/premium/moon use is
                            // not refunded here; other local-only economy gates do not have a standard
                            // refund path, and prior input validation keeps this failure path rare.
                            val authorization = economyRepository.authorizeBasicNatal(
                                requestId = newBasicNatalRequestId(),
                                languageCode = appStrings.languageCode,
                            )
                            if (!authorization.authorized) {
                                error = strings.basicNatalCalculateError
                                return@launch
                            }
                            runCatching {
                                BirthDateTimeLocal(
                                    year = date.year,
                                    month = date.monthNumber,
                                    day = date.dayOfMonth,
                                    hour = hour,
                                    minute = minute,
                                ).toUtc(birthplace.timezoneId)
                            }.mapCatching { utc ->
                                BasicNatalChartUiCalculator.calculate(
                                    birthDateTimeUtc = utc,
                                    birthLocation = BirthLocation(
                                        latitudeDegrees = birthplace.latitudeDegrees,
                                        longitudeDegrees = birthplace.longitudeDegrees,
                                    ),
                                )
                            }.onSuccess { chart ->
                                result = chart
                                economyViewModel.loadEconomy()
                            }.onFailure {
                                error = strings.basicNatalCalculateError
                            }
                        } catch (throwable: Throwable) {
                            error = when (throwable.message) {
                                "insufficient_moons" -> appStrings.economy.notEnoughMoons
                                "daily_limit" -> appStrings.economy.dailyLimitReached
                                else -> strings.basicNatalCalculateError
                            }
                            economyViewModel.loadEconomy()
                        } finally {
                            isAuthorizingBasicNatal = false
                        }
                    }
                }
                runWithEconomyGate(
                    preview = basicNatalPreview,
                    economyViewModel = economyViewModel,
                    source = "basic_natal",
                    fallbackCost = 1,
                    action = calculateAfterEconomyGate,
                )
            },
            enabled = !isAuthorizingBasicNatal,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isAuthorizingBasicNatal) strings.basicNatalCalculatingCta else strings.basicNatalCalculateCta)
        }

        result?.let { chart ->
            BasicNatalChartResultCards(chart = chart, strings = strings, appStrings = appStrings)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun BasicNatalChartResultCards(chart: NatalChartResult, strings: BirthChartStrings, appStrings: AppStrings) {
    val dimens = BWitchThemeTokens.dimens

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
        Text(strings.basicNatalPreviewTitle, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            BasicNatalChartResultCard("☀", strings.basicNatalSunLabel, chart.sunSign.toNatalDisplayName(appStrings), Modifier.weight(1f))
            BasicNatalChartResultCard("🌙", strings.basicNatalMoonLabel, chart.moonSign.toNatalDisplayName(appStrings), Modifier.weight(1f))
        }
        chart.ascendantSign?.let { ascendantSign ->
            BasicNatalChartResultCard("↗", strings.basicNatalAscendantLabel, ascendantSign.toNatalDisplayName(appStrings), Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BasicNatalChartResultCard(
    symbol: String,
    title: String,
    sign: String,
    modifier: Modifier = Modifier,
) {
    BWitchCard(
        modifier = modifier,
        contentVerticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs),
    ) {
        Text("$symbol $title", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(sign, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BirthplacePickerDialog(
    strings: BirthChartStrings,
    query: String,
    matchingBirthplaces: List<BirthplacePreset>,
    isCatalogLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (BirthplacePreset) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = BWitchThemeTokens.dimens

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
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
                verticalArrangement = Arrangement.spacedBy(spacing.spacingMd),
            ) {
                Text(text = strings.basicNatalBirthplaceLabel, style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(strings.basicNatalBirthplaceSearchLabel) },
                    supportingText = { Text(strings.basicNatalBirthplaceSearchSupportingText) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isCatalogLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = strings.basicNatalBirthplaceLoadingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (query.isNotBlank() && matchingBirthplaces.isEmpty()) {
                    Text(
                        text = strings.basicNatalBirthplaceEmptyResultsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacingXs),
                ) {
                    matchingBirthplaces.forEach { preset ->
                        OutlinedButton(
                            onClick = { onSelect(preset) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = preset.displayName(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.shareCancelCta)
                }
            }
        }
    }
}

private data class BirthplaceCatalogUiState(
    val presets: List<BirthplacePreset>,
    val isLoadingRuntimeCatalog: Boolean,
)

private fun validateBasicNatalChartInput(
    strings: BirthChartStrings,
    birthDate: LocalDate?,
    birthHour: Int?,
    birthMinute: Int?,
    selectedBirthplace: BirthplacePreset?,
): String? {
    val date = birthDate ?: return strings.basicNatalInvalidDayError
    val hour = birthHour ?: return strings.basicNatalInvalidHourError
    val minute = birthMinute ?: return strings.basicNatalInvalidMinuteError

    if (date.year !in 1..9999) return strings.basicNatalYearRangeError
    if (hour !in 0..23) return strings.basicNatalHourRangeError
    if (minute !in 0..59) return strings.basicNatalMinuteRangeError
    if (selectedBirthplace == null) return strings.basicNatalBirthplaceRequiredError

    return null
}

private fun BirthplacePreset.displayName(): String = "$cityName, $countryName"

private fun String?.isBirthEssenceEconomyError(): Boolean {
    val normalized = this?.trim()?.lowercase().orEmpty()
    if (normalized.isBlank()) return false
    return normalized.contains("insufficient_moons") || normalized.isDailyLimitError()
}

private fun String?.isDailyLimitError(): Boolean {
    val normalized = this?.trim()?.lowercase().orEmpty()
    return normalized == BIRTH_CHART_GENERATE_DAILY_LIMIT_KEY ||
        normalized.contains("daily_limit") ||
        normalized.contains("limit_reached") ||
        normalized.contains("resource_exhausted")
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
        BIRTH_CHART_GENERATE_INSUFFICIENT_MOONS_KEY -> strings.generateInsufficientMoonsError
        BIRTH_CHART_GENERATE_DAILY_LIMIT_KEY -> strings.generateDailyLimitError
        BIRTH_CHART_GENERATE_CONNECTION_KEY -> strings.generateConnectionError
        BIRTH_CHART_GENERATE_TEMPORARY_ATHENA_KEY -> strings.generateTemporaryAthenaError
        BIRTH_CHART_GENERATE_SESSION_EXPIRED_KEY -> strings.generateSessionExpiredError
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

private fun NatalZodiacSign.toNatalDisplayName(strings: AppStrings): String = when (this) {
    NatalZodiacSign.aries -> strings.zodiac.aries
    NatalZodiacSign.taurus -> strings.zodiac.taurus
    NatalZodiacSign.gemini -> strings.zodiac.gemini
    NatalZodiacSign.cancer -> strings.zodiac.cancer
    NatalZodiacSign.leo -> strings.zodiac.leo
    NatalZodiacSign.virgo -> strings.zodiac.virgo
    NatalZodiacSign.libra -> strings.zodiac.libra
    NatalZodiacSign.scorpio -> strings.zodiac.scorpio
    NatalZodiacSign.sagittarius -> strings.zodiac.sagittarius
    NatalZodiacSign.capricorn -> strings.zodiac.capricorn
    NatalZodiacSign.aquarius -> strings.zodiac.aquarius
    NatalZodiacSign.pisces -> strings.zodiac.pisces
}

@Composable
private fun ShareEssencePreviewDialog(
    essence: BirthEssenceProfile,
    appName: String,
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
                    appName = appName,
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


@OptIn(ExperimentalUuidApi::class)
private fun newBasicNatalRequestId(): String = "basic-natal-${Uuid.random()}"
