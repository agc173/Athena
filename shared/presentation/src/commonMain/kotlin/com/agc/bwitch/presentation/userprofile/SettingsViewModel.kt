package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.isActive
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.presentation.auth.SessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val username: String? = null,
    val email: String? = null,
    val birthDate: String? = null,
    val currentLanguage: AppLanguage = AppLanguage.fallback,
    val notificationsEnabled: Boolean = false,
    val dailyHoroscopeEnabled: Boolean = false,
    val ritualOfDayEnabled: Boolean = false,
    val habitsEnabled: Boolean = false,
    val appVersion: String = "",
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.Unknown,
    val subscriptionPrimaryAction: SubscriptionPrimaryAction = SubscriptionPrimaryAction.Subscribe,
    val feedback: SettingsFeedback? = null,
    val error: String? = null,
)

enum class SubscriptionPrimaryAction {
    Subscribe,
    Manage,
}

enum class SettingsFeedback {
    SubscriptionSubscribeComingSoon,
    SubscriptionManageComingSoon,
    RestorePurchasesSuccess,
    RestorePurchasesNoPurchases,
}

class SettingsViewModel(
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val sessionViewModel: SessionViewModel,
    private val observeCurrentLanguage: ObserveCurrentLanguageUseCase,
    private val observeNotificationSettings: ObserveNotificationSettingsUseCase,
    private val getNotificationSettings: GetNotificationSettingsUseCase,
    private val updateNotificationSettings: UpdateNotificationSettingsUseCase,
    private val observeSubscriptionStatus: ObserveSubscriptionStatusUseCase,
    private val getSubscriptionStatus: GetSubscriptionStatusUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        scope.launch {
            observeUserProfile()
                .catch { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
                .collect { profile ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            username = profile?.username,
                            email = profile?.email,
                            birthDate = profile?.birthDate?.toString(),
                            error = null,
                        )
                    }
                }
        }

        scope.launch {
            runCatching { getUserProfile() }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, error = error.message) } }
        }

        scope.launch {
            sessionViewModel.uiState
                .map { it.email }
                .distinctUntilChanged()
                .collect { sessionEmail ->
                    _uiState.update { current ->
                        current.copy(
                            email = current.email?.takeUnless(String::isBlank)
                                ?: sessionEmail?.takeUnless(String::isBlank),
                        )
                    }
                }
        }

        scope.launch {
            observeCurrentLanguage()
                .catch { error -> _uiState.update { it.copy(error = error.message) } }
                .collect { language ->
                    _uiState.update { it.copy(currentLanguage = language) }
                }
        }

        scope.launch {
            observeNotificationSettings()
                .catch { error -> _uiState.update { it.copy(error = error.message) } }
                .collect { notificationSettings ->
                    _uiState.update { it.copyFrom(notificationSettings) }
                }
        }

        scope.launch {
            runCatching { getNotificationSettings() }
                .onSuccess { notificationSettings ->
                    _uiState.update { it.copyFrom(notificationSettings) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }

        scope.launch {
            observeSubscriptionStatus()
                .catch { error -> _uiState.update { it.copy(error = error.message) } }
                .collect { subscriptionStatus ->
                    _uiState.update { it.copyWithSubscription(subscriptionStatus) }
                }
        }

        scope.launch {
            runCatching { getSubscriptionStatus() }
                .onSuccess { subscriptionStatus ->
                    _uiState.update { it.copyWithSubscription(subscriptionStatus) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun onNotificationsEnabledChanged(enabled: Boolean) {
        updateNotificationSettings { it.copy(globalEnabled = enabled) }
    }

    fun onDailyHoroscopeEnabledChanged(enabled: Boolean) {
        updateNotificationSettings { it.copy(dailyHoroscopeEnabled = enabled) }
    }

    fun onRitualOfDayEnabledChanged(enabled: Boolean) {
        updateNotificationSettings { it.copy(ritualOfDayEnabled = enabled) }
    }

    fun onHabitsEnabledChanged(enabled: Boolean) {
        updateNotificationSettings { it.copy(habitsEnabled = enabled) }
    }

    fun onSubscriptionPrimaryActionClicked() {
        val feedback = when (_uiState.value.subscriptionPrimaryAction) {
            SubscriptionPrimaryAction.Subscribe -> SettingsFeedback.SubscriptionSubscribeComingSoon
            SubscriptionPrimaryAction.Manage -> SettingsFeedback.SubscriptionManageComingSoon
        }
        _uiState.update { it.copy(feedback = feedback) }
    }

    fun onRestorePurchasesClicked() {
        scope.launch {
            runCatching { restorePurchases() }
                .onSuccess { result ->
                    val feedback = when (result) {
                        is RestorePurchasesResult.NoPurchasesFound -> SettingsFeedback.RestorePurchasesNoPurchases
                        is RestorePurchasesResult.Restored -> SettingsFeedback.RestorePurchasesSuccess
                    }
                    _uiState.update { it.copy(feedback = feedback) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    fun onFeedbackConsumed() {
        _uiState.update { it.copy(feedback = null) }
    }

    fun onAppVersionResolved(appVersion: String) {
        if (_uiState.value.appVersion == appVersion) return
        _uiState.update { it.copy(appVersion = appVersion) }
    }

    private fun updateNotificationSettings(update: (NotificationSettings) -> NotificationSettings) {
        scope.launch {
            val current = NotificationSettings(
                globalEnabled = _uiState.value.notificationsEnabled,
                dailyHoroscopeEnabled = _uiState.value.dailyHoroscopeEnabled,
                ritualOfDayEnabled = _uiState.value.ritualOfDayEnabled,
                habitsEnabled = _uiState.value.habitsEnabled,
            )
            runCatching {
                updateNotificationSettings(update(current))
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message) }
            }
        }
    }
}

private fun SettingsUiState.copyFrom(settings: NotificationSettings): SettingsUiState = copy(
    notificationsEnabled = settings.globalEnabled,
    dailyHoroscopeEnabled = settings.dailyHoroscopeEnabled,
    ritualOfDayEnabled = settings.ritualOfDayEnabled,
    habitsEnabled = settings.habitsEnabled,
)

private fun SettingsUiState.copyWithSubscription(subscriptionStatus: SubscriptionStatus): SettingsUiState = copy(
    subscriptionStatus = subscriptionStatus,
    subscriptionPrimaryAction = if (subscriptionStatus.isActive) {
        SubscriptionPrimaryAction.Manage
    } else {
        SubscriptionPrimaryAction.Subscribe
    },
)
