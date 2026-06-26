package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.natal.BirthDateTimeLocal
import com.agc.bwitch.domain.astrology.natal.BirthLocation
import com.agc.bwitch.domain.astrology.natal.NatalChartResult
import com.agc.bwitch.domain.astrology.natal.toUtc
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.localization.AppStrings
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
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.common.economy.DailyLimitPaywallCard
import com.agc.bwitch.ui.common.economy.EconomyGateInfoRow
import com.agc.bwitch.ui.common.economy.isDailyLimitRejected
import com.agc.bwitch.ui.common.economy.hasPremiumBenefit
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import com.agc.bwitch.ui.tarot.DeckCardUnlockRewardDialog
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

        BasicNatalChartSection()

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
private fun BasicNatalChartSection() {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    var year by remember { mutableStateOf("1990") }
    var month by remember { mutableStateOf("1") }
    var day by remember { mutableStateOf("1") }
    var hour by remember { mutableStateOf("12") }
    var minute by remember { mutableStateOf("0") }
    var timezoneOffsetMinutes by remember { mutableStateOf("0") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<NatalChartResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val validationMessage = remember(year, month, day, hour, minute, timezoneOffsetMinutes, latitude, longitude) {
        validateBasicNatalChartInput(
            year = year,
            month = month,
            day = day,
            hour = hour,
            minute = minute,
            timezoneOffsetMinutes = timezoneOffsetMinutes,
            latitude = latitude,
            longitude = longitude,
        )
    }
    val canCalculate = validationMessage == null

    BWitchCard(contentVerticalArrangement = Arrangement.spacedBy(dimens.spacingMd)) {
        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
            Text("Basic Natal Chart", style = MaterialTheme.typography.titleLarge)
            Text(
                "Start with your Sun and Moon signs using your local birth details.",
                style = MaterialTheme.typography.bodyMedium,
                color = extras.textSecondary,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            Text("Birth date", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                BasicNatalChartInput("Year", year, Modifier.weight(1.2f)) { year = it }
                BasicNatalChartInput("Month", month, Modifier.weight(1f)) { month = it }
                BasicNatalChartInput("Day", day, Modifier.weight(1f)) { day = it }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            Text("Birth time", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                BasicNatalChartInput("Hour", hour, Modifier.weight(1f)) { hour = it }
                BasicNatalChartInput("Minute", minute, Modifier.weight(1f)) { minute = it }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            Text("UTC offset", style = MaterialTheme.typography.labelLarge)
            BasicNatalChartInput(
                label = "Offset in minutes",
                value = timezoneOffsetMinutes,
                modifier = Modifier.fillMaxWidth(),
                supportingText = "Use your birth time offset from UTC. Spain in winter is 60; Spain in summer is 120.",
                onValueChange = { timezoneOffsetMinutes = it },
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            Text("Birthplace", style = MaterialTheme.typography.labelLarge)
            BasicNatalChartInput(
                label = "Birth latitude",
                value = latitude,
                modifier = Modifier.fillMaxWidth(),
                supportingText = "Positive north, negative south. Example: 40.4167",
                onValueChange = { latitude = it },
            )
            BasicNatalChartInput(
                label = "Birth longitude",
                value = longitude,
                modifier = Modifier.fillMaxWidth(),
                supportingText = "Positive east, negative west. Example: -3.7000",
                onValueChange = { longitude = it },
            )
        }

        validationMessage?.let { message ->
            Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }

        BWitchPrimaryButton(
            onClick = {
                result = null
                error = null
                runCatching {
                    BirthDateTimeLocal(
                        year = year.toInt(),
                        month = month.toInt(),
                        day = day.toInt(),
                        hour = hour.toInt(),
                        minute = minute.toInt(),
                        timezoneOffsetMinutes = timezoneOffsetMinutes.toInt(),
                    ).toUtc()
                }.mapCatching { utc ->
                    val birthLocation = if (latitude.isBlank() && longitude.isBlank()) {
                        null
                    } else {
                        BirthLocation(
                            latitudeDegrees = latitude.trim().toDouble(),
                            longitudeDegrees = longitude.trim().toDouble(),
                        )
                    }
                    BasicNatalChartUiCalculator.calculate(utc, birthLocation)
                }.onSuccess { chart ->
                    result = chart
                }.onFailure {
                    error = "We couldn't calculate this chart. Please check the date, time, UTC offset, and birthplace."
                }
            },
            enabled = canCalculate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Calculate chart")
        }

        result?.let { chart ->
            BasicNatalChartResultCards(chart)
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text(
            "This is a basic natal chart.\n\nSun, Moon, and optional Ascendant are calculated locally on your device.\n\nEnter both birthplace coordinates to include Ascendant, or leave both blank for Sun and Moon only.",
            style = MaterialTheme.typography.bodySmall,
            color = extras.textSecondary,
        )
    }
}

@Composable
private fun BasicNatalChartResultCards(chart: NatalChartResult) {
    val dimens = BWitchThemeTokens.dimens

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
        Text("Your chart preview", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            BasicNatalChartResultCard("☀", "Sun", chart.sunSign.label, Modifier.weight(1f))
            BasicNatalChartResultCard("🌙", "Moon", chart.moonSign.label, Modifier.weight(1f))
        }
        chart.ascendantSign?.let { ascendantSign ->
            BasicNatalChartResultCard("↗", "Ascendant", ascendantSign.label, Modifier.fillMaxWidth())
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
private fun BasicNatalChartInput(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = supportingText?.let { text -> ({ Text(text) }) },
        singleLine = true,
        modifier = modifier.defaultMinSize(minWidth = 0.dp),
    )
}

private fun validateBasicNatalChartInput(
    year: String,
    month: String,
    day: String,
    hour: String,
    minute: String,
    timezoneOffsetMinutes: String,
    latitude: String,
    longitude: String,
): String? {
    val parsedYear = year.toIntOrNull() ?: return "Enter a valid birth year."
    val parsedMonth = month.toIntOrNull() ?: return "Enter a valid birth month."
    val parsedDay = day.toIntOrNull() ?: return "Enter a valid birth day."
    val parsedHour = hour.toIntOrNull() ?: return "Enter a valid birth hour."
    val parsedMinute = minute.toIntOrNull() ?: return "Enter valid birth minutes."
    val parsedOffset = timezoneOffsetMinutes.toIntOrNull() ?: return "Enter a valid UTC offset in minutes."
    val hasLatitude = latitude.isNotBlank()
    val hasLongitude = longitude.isNotBlank()
    val parsedLatitude = latitude.trim().toDoubleOrNull()
    val parsedLongitude = longitude.trim().toDoubleOrNull()

    if (hasLatitude != hasLongitude) return "Enter both birth latitude and birth longitude, or leave both blank."
    if (hasLatitude && parsedLatitude == null) return "Enter a valid birth latitude."
    if (hasLongitude && parsedLongitude == null) return "Enter a valid birth longitude."

    if (parsedYear !in 1..9999) return "Birth year must be between 1 and 9999."
    if (parsedMonth !in 1..12) return "Birth month must be between 1 and 12."
    if (parsedDay !in 1..daysInMonth(parsedYear, parsedMonth)) return "Enter a valid day for this month."
    if (parsedHour !in 0..23) return "Birth hour must be between 0 and 23."
    if (parsedMinute !in 0..59) return "Birth minutes must be between 0 and 59."
    if (parsedOffset !in -18 * 60..18 * 60) return "UTC offset must be between -1080 and 1080 minutes."
    if (parsedLatitude != null && parsedLatitude !in -90.0..90.0) return "Birth latitude must be between -90 and 90."
    if (parsedLongitude != null && parsedLongitude !in -180.0..180.0) return "Birth longitude must be between -180 and 180."

    return null
}

private fun daysInMonth(year: Int, month: Int): Int = when (month) {
    2 -> if (isLeapYear(year)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun isLeapYear(year: Int): Boolean =
    year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)

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
