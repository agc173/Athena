package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.notifications.PushNotificationPreferences
import com.agc.bwitch.domain.notifications.PushPlatform
import com.agc.bwitch.domain.notifications.PushTokenRegistration
import com.agc.bwitch.domain.notifications.RegisterPushTokenUseCase
import com.agc.bwitch.domain.notifications.UpdatePushNotificationPreferencesUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.GooglePlayPurchaseState
import com.agc.bwitch.domain.settings.GetSubscriptionCatalogUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.RefreshPremiumEntitlementUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ValidateGooglePlayPurchaseUseCase
import com.agc.bwitch.domain.settings.isActive
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.presentation.auth.SessionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    NotificationsPermissionDenied,
    NotificationsUnavailable,
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

    data class AcknowledgeGooglePlayPurchase(
        val purchaseToken: String,
    ) : SettingsUiEffect

    data object RefreshEconomy : SettingsUiEffect

    data object RequestPushPermissionAndToken : SettingsUiEffect
}

sealed interface SubscriptionPurchaseOutcome {
    data class Purchased(val purchase: GooglePlayPurchase) : SubscriptionPurchaseOutcome
    data class Pending(val purchase: GooglePlayPurchase) : SubscriptionPurchaseOutcome
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
    private val refreshPremiumEntitlement: RefreshPremiumEntitlementUseCase,
    private val validateGooglePlayPurchase: ValidateGooglePlayPurchaseUseCase,
    private val registerPushToken: RegisterPushTokenUseCase,
    private val updatePushNotificationPreferences: UpdatePushNotificationPreferencesUseCase,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private val _uiEffects = MutableSharedFlow<SettingsUiEffect>(extraBufferCapacity = 16)
    val uiEffects: SharedFlow<SettingsUiEffect> = _uiEffects
    private var pendingPremiumProductId: String? = null
    private var lastRefreshedUserId: String? = null

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
            sessionViewModel.uiState
                .map { it.uid }
                .distinctUntilChanged()
                .collect { uid ->
                    if (uid.isNullOrBlank()) {
                        lastRefreshedUserId = null
                    } else if (uid != lastRefreshedUserId) {
                        lastRefreshedUserId = uid
                        refreshBackendEntitlement()
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
                        it.copy(
                            subscriptionCatalog = plans
                                .filter { plan -> plan.type == SubscriptionPlanType.Monthly }
                                .sortedForUi()
                                .map { plan -> plan.toUiPlan() },
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }

        scope.launch {
            observeSubscriptionStatus()
                .catch { error -> _uiState.update { it.copy(error = error.message) } }
                .collect { subscriptionStatus ->
                    println("BWITCH_PREMIUM_DEBUG settings_init repoStatus=$subscriptionStatus")
                    _uiState.update { it.copyWithRepositorySubscription(subscriptionStatus) }
                }
        }

        scope.launch {
            runCatching { getSubscriptionStatus() }
                .onSuccess { subscriptionStatus ->
                    _uiState.update { it.copyWithRepositorySubscription(subscriptionStatus) }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }
        }
    }

    private suspend fun refreshBackendEntitlement() {
        runCatching { refreshPremiumEntitlement() }
            .onSuccess { entitlement ->
                println("BWITCH_PREMIUM_DEBUG settings_refresh_entitlement=$entitlement")
                analyticsTracker.track(
                    AnalyticsEvent.EntitlementRefreshed(
                        status = entitlement.status.name,
                        isActive = entitlement.isActive,
                    ),
                )
                _uiState.update { current ->
                    if (entitlement.isActive) {
                        current.copyWithSubscription(entitlement.status).copy(isLoading = false, error = null)
                    } else {
                        current.copyWithSubscription(SubscriptionStatus.Inactive).copy(isLoading = false, error = null)
                    }
                }
            }
            .onFailure { error ->
                analyticsTracker.track(
                    AnalyticsEvent.EntitlementRefreshFailed(reason = error.message ?: "unknown"),
                )
                _uiState.update { it.copy(isLoading = false, error = error.message) }
            }
    }

    fun onNotificationsEnabledChanged(enabled: Boolean) {
        if (enabled) {
            updateLocalNotificationSettings { it.copy(globalEnabled = true) }
            scope.launch { _uiEffects.emit(SettingsUiEffect.RequestPushPermissionAndToken) }
            return
        }

        updateLocalAndRemoteNotificationSettings { it.copy(globalEnabled = false) }
    }

    fun onDailyHoroscopeEnabledChanged(enabled: Boolean) {
        updateLocalAndRemoteNotificationSettings { it.copy(dailyHoroscopeEnabled = enabled) }
    }

    fun onRitualOfDayEnabledChanged(enabled: Boolean) {
        updateLocalAndRemoteNotificationSettings { it.copy(ritualOfDayEnabled = enabled) }
    }

    fun onHabitsEnabledChanged(enabled: Boolean) {
        updateLocalAndRemoteNotificationSettings { it.copy(habitsEnabled = enabled) }
    }


    fun onPushPermissionAndTokenResolved(permissionGranted: Boolean, token: String?, timezone: String? = null) {
        val nextSettings = NotificationSettings(
            globalEnabled = permissionGranted && !token.isNullOrBlank(),
            dailyHoroscopeEnabled = _uiState.value.dailyHoroscopeEnabled,
            ritualOfDayEnabled = _uiState.value.ritualOfDayEnabled,
            habitsEnabled = _uiState.value.habitsEnabled,
        )
        updateLocalAndRemoteNotificationSettings(nextSettings)

        if (!permissionGranted) {
            _uiState.update { it.copy(feedback = SettingsFeedback.NotificationsPermissionDenied) }
            return
        }
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(feedback = SettingsFeedback.NotificationsUnavailable) }
            return
        }

        scope.launch {
            runCatching {
                registerPushToken(
                    PushTokenRegistration(
                        token = token,
                        platform = PushPlatform.ANDROID,
                        appVersion = _uiState.value.appVersion.takeIf { it.isNotBlank() },
                        locale = _uiState.value.currentLanguage.code,
                        timezone = timezone,
                        notificationsPermissionGranted = true,
                    ),
                )
            }.onFailure {
                _uiState.update { state -> state.copy(feedback = SettingsFeedback.NotificationsUnavailable) }
            }
        }
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
        if (_uiState.value.isLoading || plan != SubscriptionPlanSelection.Monthly) return
        analyticsTracker.track(AnalyticsEvent.PremiumCtaClicked(placement = "settings_subscribe", originPlacement = "settings"))
        val productId = KnownSubscriptionProducts.MONTHLY
        pendingPremiumProductId = productId
        analyticsTracker.track(AnalyticsEvent.PremiumPurchaseStarted(productId = productId, originPlacement = "settings"))
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            _uiEffects.emit(SettingsUiEffect.LaunchSubscriptionPurchase(plan))
        }
    }

    fun onSubscriptionPurchaseCompleted(outcome: SubscriptionPurchaseOutcome) {
        when (outcome) {
            is SubscriptionPurchaseOutcome.Purchased -> {
                when (outcome.purchase.purchaseState) {
                    GooglePlayPurchaseState.Purchased -> handlePurchasedSubscription(outcome.purchase)
                    GooglePlayPurchaseState.Pending -> handlePendingSubscriptionPurchase()
                    GooglePlayPurchaseState.Unknown -> handleUnverifiedSubscriptionPurchase()
                }
            }

            is SubscriptionPurchaseOutcome.Pending -> handlePendingSubscriptionPurchase()

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
                trackPremiumPurchaseFailed(reason = "failed")
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


    private fun handlePendingSubscriptionPurchase() {
        pendingPremiumProductId = null
        _uiState.update { it.copy(isLoading = false, error = null) }
    }

    private fun handleUnverifiedSubscriptionPurchase() {
        trackPremiumPurchaseFailed(reason = "unverified_purchase_state")
        pendingPremiumProductId = null
        _uiState.update {
            it.copy(
                isLoading = false,
                feedback = SettingsFeedback.SubscriptionPurchaseFailed,
            )
        }
    }

    private fun handlePurchasedSubscription(purchase: GooglePlayPurchase) {
        scope.launch {
            runCatching { validateGooglePlayPurchase(purchase) }
                .onSuccess { entitlement ->
                    if (entitlement.isActive) {
                        analyticsTracker.track(
                            AnalyticsEvent.PremiumPurchaseCompleted(
                                productId = purchase.productId,
                                price = null,
                                currency = null,
                            ),
                        )
                        pendingPremiumProductId = null
                        _uiState.update {
                            it.copyWithSubscription(entitlement.status).copy(isLoading = false, error = null)
                        }
                        if (!purchase.isAcknowledged) {
                            _uiEffects.emit(SettingsUiEffect.AcknowledgeGooglePlayPurchase(purchase.purchaseToken))
                        }
                        _uiEffects.emit(SettingsUiEffect.RefreshEconomy)
                    } else {
                        trackPremiumPurchaseFailed(reason = "backend_inactive")
                        pendingPremiumProductId = null
                        _uiState.update {
                            it.copy(isLoading = false, feedback = SettingsFeedback.SubscriptionPurchaseFailed)
                        }
                    }
                }
                .onFailure { error ->
                    trackPremiumPurchaseFailed(reason = "backend_validation_failed")
                    pendingPremiumProductId = null
                    _uiState.update {
                        it.copy(isLoading = false, feedback = SettingsFeedback.SubscriptionPurchaseFailed, error = error.message)
                    }
                }
        }
    }

    private fun trackPremiumPurchaseFailed(reason: String) {
        analyticsTracker.track(
            AnalyticsEvent.PremiumPurchaseFailed(
                productId = pendingPremiumProductId ?: _uiState.value.resolveManageSubscriptionProductId().orEmpty(),
                reason = reason,
            ),
        )
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
        analyticsTracker.track(AnalyticsEvent.PremiumRestoreClicked(placement = "settings_restore"))
        scope.launch {
            runCatching { restorePurchases() }
                .onSuccess { result ->
                    val backendStatus = runCatching { refreshPremiumEntitlement().status }.getOrNull()
                    val effectiveStatus = when {
                        backendStatus?.isActive == true -> backendStatus
                        result is RestorePurchasesResult.Restored -> result.status
                        else -> _uiState.value.subscriptionStatus
                    }
                    val feedback = when {
                        effectiveStatus.isActive -> {
                            analyticsTracker.track(AnalyticsEvent.PremiumRestoreCompleted(status = effectiveStatus.name))
                            SettingsFeedback.RestorePurchasesSuccess
                        }
                        else -> when (result) {
                        is RestorePurchasesResult.NoPurchasesFound -> {
                            analyticsTracker.track(AnalyticsEvent.PremiumRestoreEmpty(reason = "no_purchases_or_backend_inactive"))
                            SettingsFeedback.RestorePurchasesNoPurchases
                        }

                        is RestorePurchasesResult.Restored -> {
                            analyticsTracker.track(AnalyticsEvent.PremiumRestoreEmpty(reason = "backend_inactive"))
                            SettingsFeedback.RestorePurchasesNoPurchases
                        }
                    }
                    }
                    println("BWITCH_PREMIUM_DEBUG restore_feedback result=$result backendStatus=$backendStatus effectiveStatus=$effectiveStatus feedback=$feedback")
                    _uiState.update { state ->
                        val next = state.copyWithSubscription(effectiveStatus)
                        next.copy(feedback = feedback)
                    }
                    if (effectiveStatus.isActive) {
                        _uiEffects.emit(SettingsUiEffect.RefreshEconomy)
                    }
                }
                .onFailure { error ->
                    analyticsTracker.track(AnalyticsEvent.PremiumRestoreEmpty(reason = error.message ?: "restore_failed"))
                    println("BWITCH_PREMIUM_DEBUG restore_feedback error=${error.message}")
                    _uiState.update { it.copy(error = error.message, feedback = SettingsFeedback.SubscriptionPurchaseFailed) }
                }
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

    fun clear() {
        scope.cancel()
    }

    private fun updateLocalNotificationSettings(update: (NotificationSettings) -> NotificationSettings) {
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

    private fun updateLocalAndRemoteNotificationSettings(update: (NotificationSettings) -> NotificationSettings) {
        scope.launch {
            val current = NotificationSettings(
                globalEnabled = _uiState.value.notificationsEnabled,
                dailyHoroscopeEnabled = _uiState.value.dailyHoroscopeEnabled,
                ritualOfDayEnabled = _uiState.value.ritualOfDayEnabled,
                habitsEnabled = _uiState.value.habitsEnabled,
            )
            val next = update(current)
            updateLocalAndRemoteNotificationSettings(next)
        }
    }

    private fun updateLocalAndRemoteNotificationSettings(next: NotificationSettings) {
        scope.launch {
            runCatching { updateNotificationSettings(next) }
                .onFailure { error -> _uiState.update { it.copy(error = error.message) } }

            runCatching { updatePushNotificationPreferences(next.toPushNotificationPreferences()) }
                .onFailure { _uiState.update { it.copy(feedback = SettingsFeedback.NotificationsUnavailable) } }
        }
    }
}

private fun NotificationSettings.toPushNotificationPreferences(): PushNotificationPreferences = PushNotificationPreferences(
    globalEnabled = globalEnabled,
    dailyHoroscopeEnabled = dailyHoroscopeEnabled,
    dailyRewardEnabled = false,
    tarotOracleReminderEnabled = false,
    ritualsEnabled = ritualOfDayEnabled,
    habitsEnabled = habitsEnabled,
)

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


private fun SettingsUiState.copyWithRepositorySubscription(subscriptionStatus: SubscriptionStatus): SettingsUiState =
    copyWithSubscription(subscriptionStatus)

private fun SubscriptionPlan.toUiPlan(): SubscriptionPlanUi = SubscriptionPlanUi(
    productId = productId,
    title = title,
    formattedPrice = formattedPrice,
    type = when (type) {
        SubscriptionPlanType.Monthly -> SubscriptionPlanSelection.Monthly
        SubscriptionPlanType.Annual, SubscriptionPlanType.Unknown -> null
    },
)

private fun SettingsUiState.resolveManageSubscriptionProductId(): String? {
    val preferredType = when (subscriptionStatus) {
        SubscriptionStatus.ActiveMonthly -> SubscriptionPlanSelection.Monthly
        SubscriptionStatus.ActiveAnnual -> null
        SubscriptionStatus.Unknown,
        SubscriptionStatus.Inactive,
        -> null
    }

    if (preferredType != null) {
        subscriptionCatalog.firstOrNull { it.type == preferredType }?.productId?.let { return it }
    }

    return when (subscriptionStatus) {
        SubscriptionStatus.ActiveMonthly -> KnownSubscriptionProducts.MONTHLY
        SubscriptionStatus.ActiveAnnual -> null
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
