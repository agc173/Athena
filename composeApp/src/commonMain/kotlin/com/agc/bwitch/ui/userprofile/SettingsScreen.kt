package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agc.bwitch.config.SettingsLinks
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.localization.SettingsStrings
import com.agc.bwitch.platform.getAppVersionLabel
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.presentation.userprofile.SettingsFeedback
import com.agc.bwitch.presentation.userprofile.SubscriptionPlanUi
import com.agc.bwitch.presentation.userprofile.SettingsUiEffect
import com.agc.bwitch.presentation.userprofile.SettingsViewModel
import com.agc.bwitch.presentation.userprofile.SubscriptionPrimaryAction
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.premium.PremiumCard
import com.agc.bwitch.ui.common.premium.PremiumBenefitsList
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val settingsVm: SettingsViewModel = koinInject()
    val sessionVm: SessionViewModel = koinInject()
    val appLanguageVm: AppLanguageViewModel = koinInject()
    val economyVm: EconomyViewModel = koinInject()
    val clearLocalUserData: ClearLocalUserDataUseCase = koinInject()

    val settingsState by settingsVm.uiState.collectAsState()

    val strings = appStrings.settings
    val commonStrings = appStrings.common
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val appVersionLabel = remember { getAppVersionLabel() }
    val uriHandler = LocalUriHandler.current
    val purchaseLauncher = rememberSubscriptionPurchaseLauncher()
    val managementLauncher = rememberSubscriptionManagementLauncher()
    val handlePushPermissionRequest = rememberHandlePushPermissionRequest(settingsVm)
    val syncPushPermissionState = rememberSyncPushPermissionState(settingsVm)
    val handleSecureSignOut = rememberHandleSecureSignOut(sessionVm, clearLocalUserData)

    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionPlanDialog by rememberSaveable { mutableStateOf(false) }

    val username = settingsState.username?.takeUnless { it.isBlank() } ?: strings.notAvailable
    val email = settingsState.email?.takeUnless { it.isBlank() } ?: strings.notAvailable
    val birthDate = settingsState.birthDate ?: strings.notAvailable

    LaunchedEffect(appVersionLabel) {
        settingsVm.onAppVersionResolved(appVersionLabel)
    }

    LaunchedEffect(Unit) {
        syncPushPermissionState()
    }

    LaunchedEffect(settingsState.error) {
        val error = settingsState.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(error)
    }
    LaunchedEffect(settingsState.subscriptionPrimaryAction) {
        if (settingsState.subscriptionPrimaryAction == SubscriptionPrimaryAction.Subscribe) {
            settingsVm.onPremiumCtaShown("settings_subscribe")
        }
    }

    LaunchedEffect(settingsState.feedback) {
        val feedback = settingsState.feedback ?: return@LaunchedEffect
        val message = when (feedback) {
            SettingsFeedback.SubscriptionSubscribeComingSoon -> strings.subscriptionSubscribeComingSoon
            SettingsFeedback.SubscriptionManageComingSoon -> strings.subscriptionManageComingSoon
            SettingsFeedback.SubscriptionPurchaseFailed -> strings.subscriptionPurchaseFailed
            SettingsFeedback.RestorePurchasesSuccess -> strings.subscriptionRestoreSuccess
            SettingsFeedback.RestorePurchasesNoPurchases -> strings.subscriptionRestoreNoPurchases
            SettingsFeedback.DeleteAccountRequested -> strings.deleteAccountRequestedFeedback
            SettingsFeedback.NotificationsPermissionDenied -> strings.comingSoon
            SettingsFeedback.NotificationsUnavailable -> strings.comingSoon
        }
        snackbarHostState.showSnackbar(message)
        settingsVm.onFeedbackConsumed()
    }

    LaunchedEffect(Unit) {
        settingsVm.uiEffects.collect { effect ->
            when (effect) {
                is SettingsUiEffect.LaunchSubscriptionPurchase -> {
                    val outcome = purchaseLauncher.launch(effect.plan)
                    settingsVm.onSubscriptionPurchaseCompleted(outcome)
                }

                is SettingsUiEffect.LaunchSubscriptionPurchaseWithProduct -> {
                    val outcome = purchaseLauncher.launch(effect.productId)
                    settingsVm.onSubscriptionPurchaseCompleted(outcome)
                }

                is SettingsUiEffect.LaunchManageSubscription -> {
                    val outcome = managementLauncher.launch(effect.productId)
                    settingsVm.onSubscriptionManagementCompleted(outcome)
                }

                is SettingsUiEffect.AcknowledgeGooglePlayPurchase -> {
                    purchaseLauncher.acknowledge(effect.purchaseToken)
                }

                SettingsUiEffect.RefreshEconomy -> {
                    economyVm.loadEconomy()
                }

                SettingsUiEffect.RequestPushPermissionAndToken -> {
                    handlePushPermissionRequest()
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            title = strings.languageDialogTitle,
            closeLabel = strings.close,
            selectedPrefix = commonStrings.languageSelectedPrefix,
            currentLanguage = settingsState.currentLanguage,
            supportedLanguages = AppLanguage.supported,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                appLanguageVm.onLanguageSelected(language)
                showLanguageDialog = false
            },
        )
    }

    if (showSubscriptionPlanDialog) {
        SubscriptionPlanDialog(
            title = strings.subscriptionActionSubscribe,
            closeLabel = strings.close,
            plans = settingsState.subscriptionCatalog,
            monthlyLabelFallback = strings.subscriptionPlanMonthlyLabel,
            onDismiss = { showSubscriptionPlanDialog = false },
            onCatalogPlanSelected = { plan ->
                showSubscriptionPlanDialog = false
                settingsVm.onCatalogSubscriptionSelected(plan.productId)
            },
        )
    }

    if (settingsState.isDeleteAccountConfirmationVisible) {
        DeleteAccountConfirmationDialog(
            title = strings.deleteAccountDialogTitle,
            message = strings.deleteAccountDialogMessage,
            dismissLabel = strings.deleteAccountDialogCancel,
            confirmLabel = strings.deleteAccountDialogConfirm,
            loadingLabel = strings.deleteAccountDeleting,
            isLoading = settingsState.isDeletingAccount,
            onDismiss = settingsVm::onDeleteAccountConfirmationDismissed,
            onConfirm = settingsVm::onDeleteAccountConfirmed,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BWitchScreen(
            contentPadding = contentPadding,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        ) {
            SettingsSectionCard(title = strings.sectionAccount) {
                SettingsRow(label = strings.username, value = username)
                SettingsRow(label = strings.email, value = email)
                SettingsRow(label = strings.birthDate, value = birthDate)
                SettingsRow(
                    label = strings.language,
                    value = settingsState.currentLanguage.nativeLabel,
                    onClick = { showLanguageDialog = true },
                )
                SettingsRow(
                    label = strings.signOut,
                    isDestructive = true,
                    showDivider = false,
                    onClick = {
                        scope.launch {
                            runCatching { handleSecureSignOut() }
                        }
                    },
                )
            }

            SettingsSectionCard(title = strings.sectionNotifications) {
                SettingsSwitchRow(
                    label = strings.notificationsEnabled,
                    checked = settingsState.notificationsEnabled,
                    onCheckedChange = settingsVm::onNotificationsEnabledChanged,
                )
                SettingsSwitchRow(
                    label = strings.dailyHoroscope,
                    checked = settingsState.dailyHoroscopeEnabled,
                    enabled = settingsState.notificationsEnabled,
                    onCheckedChange = settingsVm::onDailyHoroscopeEnabledChanged,
                )
                SettingsSwitchRow(
                    label = strings.ritualOfDay,
                    checked = settingsState.ritualOfDayEnabled,
                    enabled = settingsState.notificationsEnabled,
                    onCheckedChange = settingsVm::onRitualOfDayEnabledChanged,
                )
                SettingsSwitchRow(
                    label = strings.habits,
                    checked = settingsState.habitsEnabled,
                    enabled = settingsState.notificationsEnabled,
                    onCheckedChange = settingsVm::onHabitsEnabledChanged,
                )
            }

            PremiumBenefitsList(
                title = appStrings.premiumBenefits.title,
                subtitle = appStrings.premiumBenefits.subtitle,
                bullets = appStrings.premiumBenefits.bullets,
                disclaimer = appStrings.premiumBenefits.disclaimer,
            )

            PremiumCard(
                title = strings.sectionPurchasesSubscription,
                statusLabel = settingsState.subscriptionStatus.toLocalizedLabel(strings),
                primaryActionLabel = when (settingsState.subscriptionPrimaryAction) {
                    SubscriptionPrimaryAction.Subscribe -> strings.subscriptionActionSubscribe
                    SubscriptionPrimaryAction.Manage -> strings.subscriptionActionManage
                },
                restoreActionLabel = strings.restorePurchases,
                onPrimaryActionClick = {
                    if (settingsState.subscriptionPrimaryAction == SubscriptionPrimaryAction.Subscribe) {
                        showSubscriptionPlanDialog = true
                    } else {
                        settingsVm.onSubscriptionPrimaryActionClicked()
                    }
                },
                onRestoreActionClick = settingsVm::onRestorePurchasesClicked,
            )

            SettingsSectionCard(title = strings.sectionHelpSupport) {
                SettingsRow(
                    label = strings.contactSupport,
                    onClick = {
                        runCatching { uriHandler.openUri(SettingsLinks.contactSupportMailto()) }
                            .onFailure { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } }
                    },
                )
                SettingsRow(
                    label = strings.reportIssue,
                    onClick = {
                        runCatching { uriHandler.openUri(SettingsLinks.reportIssueMailto()) }
                            .onFailure { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } }
                    },
                )
                SettingsRow(
                    label = strings.appVersion,
                    value = settingsState.appVersion,
                    showDivider = false,
                )
            }

            SettingsSectionCard(title = strings.sectionPrivacyLegal) {
                SettingsRow(
                    label = strings.privacyPolicy,
                    onClick = {
                        runCatching { uriHandler.openUri(SettingsLinks.privacyPolicyUrl) }
                            .onFailure { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } }
                    },
                )
                SettingsRow(
                    label = strings.termsAndConditions,
                    showDivider = false,
                    onClick = {
                        runCatching { uriHandler.openUri(SettingsLinks.termsAndConditionsUrl) }
                            .onFailure { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } }
                    },
                )
            }



            SettingsSectionCard(
                title = strings.sectionDangerZone,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
            ) {
                SettingsRow(
                    label = strings.deleteAccount,
                    isDestructive = true,
                    showDivider = false,
                    onClick = if (settingsState.isDeletingAccount) null else settingsVm::onDeleteAccountClicked,
                )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(contentPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun DeleteAccountConfirmationDialog(
    title: String,
    message: String,
    dismissLabel: String,
    confirmLabel: String,
    loadingLabel: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(text = title) },
        text = { Text(text = message) },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text(dismissLabel)
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isLoading) {
                Text(if (isLoading) loadingLabel else confirmLabel)
            }
        },
    )
}

@Composable
private fun LanguageSelectorDialog(
    title: String,
    closeLabel: String,
    selectedPrefix: String,
    currentLanguage: AppLanguage,
    supportedLanguages: List<AppLanguage>,
    onDismiss: () -> Unit,
    onLanguageSelected: (AppLanguage) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                supportedLanguages.forEach { language ->
                    val label = if (language == currentLanguage) {
                        selectedPrefix + language.nativeLabel
                    } else {
                        language.nativeLabel
                    }
                    Text(
                        text = label,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language) }
                            .padding(vertical = 10.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(closeLabel)
            }
        },
    )
}

@Composable
private fun SubscriptionPlanDialog(
    title: String,
    closeLabel: String,
    plans: List<SubscriptionPlanUi>,
    monthlyLabelFallback: String,
    onDismiss: () -> Unit,
    onCatalogPlanSelected: (SubscriptionPlanUi) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (plans.isNotEmpty()) {
                    plans.forEach { plan ->
                        TextButton(
                            onClick = { onCatalogPlanSelected(plan) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val label = if (plan.formattedPrice.isBlank()) {
                                plan.title
                            } else {
                                "${plan.title} · ${plan.formattedPrice}"
                            }
                            Text(text = label)
                        }
                    }
                } else {
                    Text(
                        text = monthlyLabelFallback,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(closeLabel)
            }
        },
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    colors: androidx.compose.material3.CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
    ),
    content: @Composable () -> Unit,
) {
    BWitchCard(
        modifier = Modifier.fillMaxWidth(),
        colors = colors,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        content()
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String? = null,
    isDestructive: Boolean = false,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val contentColor = if (isDestructive) MaterialTheme.colorScheme.error else Color.Unspecified
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Column {
        Row(
            modifier = rowModifier.padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                )
                if (!value.isNullOrBlank()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
            )
        }
        if (showDivider) {
            HorizontalDivider()
        }
    }
}

private fun SubscriptionStatus.toLocalizedLabel(strings: SettingsStrings): String = when (this) {
    SubscriptionStatus.Unknown -> strings.subscriptionStatusUnknown
    SubscriptionStatus.Inactive -> strings.subscriptionStatusInactive
    SubscriptionStatus.ActiveMonthly -> strings.subscriptionStatusActiveMonthly
    SubscriptionStatus.ActiveAnnual -> strings.subscriptionStatusActiveAnnual
}
