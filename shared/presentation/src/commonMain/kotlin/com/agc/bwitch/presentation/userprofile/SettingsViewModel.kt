package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionCatalogUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.isActive
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.presentation.auth.SessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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
    val subscriptionCatalog: List<SubscriptionPlanUi> = emptyList(),
    val subscriptionPrimaryAction: SubscriptionPrimaryAction = SubscriptionPrimaryAction.Subscribe,
    val isDeleteAccountConfirmationVisible: Boolean = false,
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
    SubscriptionPurchaseFailed,
    RestorePurchasesSuccess,
    RestorePurchasesNoPurchases,
    DeleteAccountComingSoon,
}

data class SubscriptionPlanUi(
    val productId: String,
    val title: String,
    val formattedPrice: String,
    val type: SubscriptionPlanSelection?,
)

enum class SubscriptionPlanSelection {
    Monthly,
    Annual,
}

sealed interface SettingsUiEffect {
    data class LaunchSubscriptionPurchase(
        val plan: SubscriptionPlanSelection,
    ) : SettingsUiEffect

    data class LaunchSubscriptionPurchaseWithProduct(
        val productId: String,
    ) : SettingsUiEffect

    data class LaunchManageSubscription(
        val productId: String?,
    ) : SettingsUiEffect
}

sealed interface SubscriptionPurchaseOutcome {
    data object Success : SubscriptionPurchaseOutcome
    data object Cancelled : SubscriptionPurchaseOutcome
    data object Unsupported : SubscriptionPurchaseOutcome
    data object Failed : SubscriptionPurchaseOutcome
}

sealed interface SubscriptionManagementOutcome {
    data object Opened : SubscriptionManagementOutcome
    data object Unsupported : SubscriptionManagementOutcome
    data object Failed : SubscriptionManagementOutcome
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
    private val getSubscriptionCatalog: GetSubscriptionCatalogUseCase,
    private val restorePurchases: RestorePurchasesUseCase,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _uiEffects = MutableSharedFlow<SettingsUiEffect>()
    val uiEffects: SharedFlow<SettingsUiEffect> = _uiEffects
    private var pendingPremiumProductId: String? = null

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
            runCatching { getSubscriptionCatalog() }
                .onSuccess { plans ->
                    _uiState.update {
                        it.copy(subscriptionCatalog = plans.sortedForUi().map { plan -> plan.toUiPlan() })
                    }
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
        if (_uiState.value.isLoading) return
        analyticsTracker.track(AnalyticsEvent.PremiumCtaClicked(placement = "settings_primary", originPlacement = "settings"))
        when (_uiState.value.subscriptionPrimaryAction) {
            SubscriptionPrimaryAction.Subscribe -> onSubscribeClicked()
            SubscriptionPrimaryAction.Manage -> {
                pendingPremiumProductId = null
                scope.launch {
                    _uiEffects.emit(
                        SettingsUiEffect.LaunchManageSubscription(
                            productId = _uiState.value.resolveManageSubscriptionProductId(),
                        ),
                    )
                }
            }
        }
    }

    fun onSubscribeClicked(plan: SubscriptionPlanSelection = SubscriptionPlanSelection.Monthly) {
        if (_uiState.value.isLoading) return
        analyticsTracker.track(AnalyticsEvent.PremiumCtaClicked(placement = "settings_subscribe", originPlacement = "settings"))
        val productId = when (plan) {
            SubscriptionPlanSelection.Monthly -> KnownSubscriptionProducts.MONTHLY
            SubscriptionPlanSelection.Annual -> KnownSubscriptionProducts.ANNUAL
        }
        pendingPremiumProductId = productId
        analyticsTracker.track(AnalyticsEvent.PremiumPurchaseStarted(productId = productId, originPlacement = "settings"))
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _uiEffects.emit(SettingsUiEffect.LaunchSubscriptionPurchase(plan))
        }
    }

    fun onSubscriptionPurchaseCompleted(outcome: SubscriptionPurchaseOutcome) {
        when (outcome) {
            SubscriptionPurchaseOutcome.Success -> {
                val productId = pendingPremiumProductId ?: _uiState.value.resolveManageSubscriptionProductId().orEmpty()
                analyticsTracker.track(
                    AnalyticsEvent.PremiumPurchaseCompleted(
                        productId = productId,
                        price = null,
                        currency = null,
                    ),
                )
                pendingPremiumProductId = null
                scope.launch {
                    runCatching { getSubscriptionStatus() }
                        .onSuccess { subscriptionStatus ->
                            _uiState.update {
                                it.copyWithSubscription(subscriptionStatus).copy(isLoading = false, error = null)
                            }
                        }
                        .onFailure { error ->
                            _uiState.update { it.copy(isLoading = false, error = error.message) }
                        }
                }
            }

            SubscriptionPurchaseOutcome.Cancelled -> {
                pendingPremiumProductId = null
                _uiState.update { it.copy(isLoading = false) }
            }

            SubscriptionPurchaseOutcome.Unsupported -> {
                pendingPremiumProductId = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedback = SettingsFeedback.SubscriptionSubscribeComingSoon,
                    )
                }
            }

            SubscriptionPurchaseOutcome.Failed -> {
                analyticsTracker.track(
                    AnalyticsEvent.PremiumPurchaseFailed(
                        productId = pendingPremiumProductId ?: _uiState.value.resolveManageSubscriptionProductId().orEmpty(),
                        reason = "failed",
                    ),
                )
                pendingPremiumProductId = null
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedback = SettingsFeedback.SubscriptionPurchaseFailed,
                    )
                }
            }
        }
    }

    fun onCatalogSubscriptionSelected(productId: String) {
        if (_uiState.value.isLoading) return
        analyticsTracker.track(AnalyticsEvent.PremiumCtaClicked(placement = "settings_catalog", originPlacement = "settings"))
        pendingPremiumProductId = productId
        analyticsTracker.track(AnalyticsEvent.PremiumPurchaseStarted(productId = productId, originPlacement = "settings"))
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _uiEffects.emit(SettingsUiEffect.LaunchSubscriptionPurchaseWithProduct(productId))
        }
    }

    fun onPremiumCtaShown(placement: String) {
        analyticsTracker.track(AnalyticsEvent.PremiumCtaShown(placement = placement, originPlacement = "settings"))
    }

    fun onSubscriptionManagementCompleted(outcome: SubscriptionManagementOutcome) {
        when (outcome) {
            SubscriptionManagementOutcome.Opened -> _uiState.update { it.copy(isLoading = false) }
            SubscriptionManagementOutcome.Unsupported,
            SubscriptionManagementOutcome.Failed,
            -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedback = SettingsFeedback.SubscriptionManageComingSoon,
                    )
                }
            }
        }
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

    fun onDeleteAccountClicked() {
        _uiState.update { it.copy(isDeleteAccountConfirmationVisible = true) }
    }

    fun onDeleteAccountConfirmationDismissed() {
        _uiState.update { it.copy(isDeleteAccountConfirmationVisible = false) }
    }

    fun onDeleteAccountConfirmed() {
        _uiState.update {
            it.copy(
                isDeleteAccountConfirmationVisible = false,
                feedback = SettingsFeedback.DeleteAccountComingSoon,
            )
        }
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

private fun SubscriptionPlan.toUiPlan(): SubscriptionPlanUi = SubscriptionPlanUi(
    productId = productId,
    title = title,
    formattedPrice = formattedPrice,
    type = when (type) {
        SubscriptionPlanType.Monthly -> SubscriptionPlanSelection.Monthly
        SubscriptionPlanType.Annual -> SubscriptionPlanSelection.Annual
        SubscriptionPlanType.Unknown -> null
    },
)

private fun SettingsUiState.resolveManageSubscriptionProductId(): String? {
    val preferredType = when (subscriptionStatus) {
        SubscriptionStatus.ActiveMonthly -> SubscriptionPlanSelection.Monthly
        SubscriptionStatus.ActiveAnnual -> SubscriptionPlanSelection.Annual
        SubscriptionStatus.Unknown,
        SubscriptionStatus.Inactive,
        -> null
    }

    if (preferredType != null) {
        subscriptionCatalog.firstOrNull { it.type == preferredType }?.productId?.let { return it }
    }

    return when (subscriptionStatus) {
        SubscriptionStatus.ActiveMonthly -> KnownSubscriptionProducts.MONTHLY
        SubscriptionStatus.ActiveAnnual -> KnownSubscriptionProducts.ANNUAL
        SubscriptionStatus.Unknown,
        SubscriptionStatus.Inactive,
        -> null
    }
}

private fun List<SubscriptionPlan>.sortedForUi(): List<SubscriptionPlan> = sortedBy { plan ->
    when (plan.type) {
        SubscriptionPlanType.Monthly -> 0
        SubscriptionPlanType.Annual -> 1
        SubscriptionPlanType.Unknown -> 2
    }
}
