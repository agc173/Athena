package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
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
    val hasActiveSubscription: Boolean = false,
    val error: String? = null,
)

class SettingsViewModel(
    private val observeUserProfile: ObserveUserProfileUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val sessionViewModel: SessionViewModel,
    private val observeCurrentLanguage: ObserveCurrentLanguageUseCase,
    private val observeNotificationSettings: ObserveNotificationSettingsUseCase,
    private val getNotificationSettings: GetNotificationSettingsUseCase,
    private val updateNotificationSettings: UpdateNotificationSettingsUseCase,
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
