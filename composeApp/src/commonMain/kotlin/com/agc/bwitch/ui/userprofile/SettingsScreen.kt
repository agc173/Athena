package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.platform.getAppVersionLabel
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val profileVm: UserProfileViewModel = koinInject()
    val sessionVm: SessionViewModel = koinInject()
    val appLanguageVm: AppLanguageViewModel = koinInject()
    val clearLocalUserData: ClearLocalUserDataUseCase = koinInject()

    val profileState by profileVm.uiState.collectAsState()
    val sessionState by sessionVm.uiState.collectAsState()
    val languageState by appLanguageVm.uiState.collectAsState()

    val strings = appStrings.settings
    val commonStrings = appStrings.common
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var notificationsEnabled by rememberSaveable { mutableStateOf(false) }
    var dailyHoroscopeEnabled by rememberSaveable { mutableStateOf(false) }
    var ritualOfDayEnabled by rememberSaveable { mutableStateOf(false) }
    var habitsEnabled by rememberSaveable { mutableStateOf(false) }
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }

    val profile = profileState.profile
    val username = profile?.username?.takeUnless { it.isBlank() } ?: strings.notAvailable
    val email = profile?.email?.takeUnless { it.isBlank() }
        ?: sessionState.email?.takeUnless { it.isBlank() }
        ?: strings.notAvailable
    val birthDate = profile?.birthDate?.toString() ?: strings.notAvailable
    val appVersion = getAppVersionLabel()
    val hasActiveSubscription = false

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            title = strings.languageDialogTitle,
            closeLabel = strings.close,
            selectedPrefix = commonStrings.languageSelectedPrefix,
            currentLanguage = languageState.currentLanguage,
            supportedLanguages = languageState.supportedLanguages,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { language ->
                appLanguageVm.onLanguageSelected(language)
                showLanguageDialog = false
            },
        )
    }

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        BWitchSectionHeader(
            title = appStrings.navigation.settings,
            subtitle = strings.subtitle,
        )

        SettingsSectionCard(title = strings.sectionAccount) {
            SettingsRow(label = strings.username, value = username)
            SettingsRow(label = strings.email, value = email)
            SettingsRow(label = strings.birthDate, value = birthDate)
            SettingsRow(
                label = strings.language,
                value = languageState.currentLanguage.nativeLabel,
                onClick = { showLanguageDialog = true },
            )
            SettingsRow(
                label = strings.signOut,
                isDestructive = true,
                showDivider = false,
                onClick = {
                    scope.launch {
                        runCatching { sessionVm.signOut() }
                        runCatching { clearLocalUserData() }
                    }
                },
            )
        }

        SettingsSectionCard(title = strings.sectionNotifications) {
            SettingsSwitchRow(
                label = strings.notificationsEnabled,
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it },
            )
            SettingsSwitchRow(
                label = strings.dailyHoroscope,
                checked = dailyHoroscopeEnabled,
                enabled = notificationsEnabled,
                onCheckedChange = { dailyHoroscopeEnabled = it },
            )
            SettingsSwitchRow(
                label = strings.ritualOfDay,
                checked = ritualOfDayEnabled,
                enabled = notificationsEnabled,
                onCheckedChange = { ritualOfDayEnabled = it },
            )
            SettingsSwitchRow(
                label = strings.habits,
                checked = habitsEnabled,
                enabled = notificationsEnabled,
                showDivider = false,
                onCheckedChange = { habitsEnabled = it },
            )
        }

        SettingsSectionCard(title = strings.sectionPurchasesSubscription) {
            SettingsRow(label = strings.subscriptionStatus, value = strings.subscriptionStatusFree)
            SettingsRow(
                label = if (hasActiveSubscription) strings.subscriptionActionManage else strings.subscriptionActionSubscribe,
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
            )
            SettingsRow(
                label = strings.restorePurchases,
                showDivider = false,
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
            )
        }

        SettingsSectionCard(title = strings.sectionHelpSupport) {
            SettingsRow(
                label = strings.contactSupport,
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
            )
            SettingsRow(
                label = strings.reportIssue,
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
            )
            SettingsRow(
                label = strings.appVersion,
                value = appVersion,
                showDivider = false,
            )
        }

        SettingsSectionCard(title = strings.sectionPrivacyLegal) {
            SettingsRow(
                label = strings.privacyPolicy,
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
            )
            SettingsRow(
                label = strings.termsAndConditions,
                showDivider = false,
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
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
                onClick = { scope.launch { snackbarHostState.showSnackbar(strings.comingSoon) } },
            )
        }

        SnackbarHost(hostState = snackbarHostState)
    }
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
