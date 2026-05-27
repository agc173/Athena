package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.auth.AuthUser
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.notifications.PushNotificationPreferences
import com.agc.bwitch.domain.notifications.PushPlatform
import com.agc.bwitch.domain.notifications.PushRegistrationRepository
import com.agc.bwitch.domain.notifications.PushTokenRegistration
import com.agc.bwitch.domain.notifications.PushTestNotificationRepository
import com.agc.bwitch.domain.notifications.RegisterPushTokenUseCase
import com.agc.bwitch.domain.notifications.SendTestNotificationUseCase
import com.agc.bwitch.domain.notifications.UpdatePushNotificationPreferencesUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionCatalogUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.GooglePlayPurchaseState
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.NotificationSettingsRepository
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.PremiumEntitlement
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.RefreshPremiumEntitlementUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ValidateGooglePlayPurchaseUseCase
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import com.agc.bwitch.presentation.analytics.FakeAnalyticsTracker
import com.agc.bwitch.presentation.auth.SessionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelAnalyticsTest {

    @Test
    fun `premium CTA click and backend active purchase emit completed analytics and refresh economy`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val entitlements = FakePremiumEntitlementRepository(
                validateEntitlement = PremiumEntitlement(isActive = true, status = SubscriptionStatus.ActiveMonthly),
            )
            val viewModel = viewModel(analytics = analytics, entitlements = entitlements)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onSubscribeClicked()
            viewModel.onPremiumCtaShown("settings_subscribe")
            advanceUntilIdle()

            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(googlePlayPurchase()))
            advanceUntilIdle()

            val premiumClickedEvents = analytics.events.filterIsInstance<AnalyticsEvent.PremiumCtaClicked>()
            val premiumShownEvents = analytics.events.filterIsInstance<AnalyticsEvent.PremiumCtaShown>()
            val premiumStartedEvents = analytics.events.filterIsInstance<AnalyticsEvent.PremiumPurchaseStarted>()

            assertEquals(1, entitlements.validateCallCount)
            assertEquals("token-123", entitlements.lastValidatedPurchase?.purchaseToken)
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertTrue(effects.any { it is SettingsUiEffect.LaunchSubscriptionPurchase })
            assertTrue(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertTrue(effects.any { it is SettingsUiEffect.AcknowledgeGooglePlayPurchase })
            assertEquals(SubscriptionStatus.ActiveMonthly, viewModel.uiState.value.subscriptionStatus)
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumCtaClicked })
            assertTrue(premiumShownEvents.isNotEmpty())
            assertTrue(premiumStartedEvents.isNotEmpty())
            assertTrue(premiumClickedEvents.all { it.originPlacement == "settings" })
            assertTrue(premiumShownEvents.all { it.originPlacement == "settings" })
            assertTrue(premiumStartedEvents.all { it.originPlacement == "settings" })
            assertEquals("settings", premiumClickedEvents.first().originPlacement)
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `premium_purchase_completed no se emite si backend no activa entitlement`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = viewModel(
                analytics = analytics,
                entitlements = FakePremiumEntitlementRepository(
                    validateEntitlement = PremiumEntitlement(isActive = false, status = SubscriptionStatus.Inactive),
                ),
            )

            advanceUntilIdle()
            viewModel.onSubscribeClicked()
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(googlePlayPurchase()))
            advanceUntilIdle()

            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseFailed })
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pending purchase no activa premium ni emite completed ni refresh economy`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val entitlements = FakePremiumEntitlementRepository()
            val viewModel = viewModel(analytics = analytics, entitlements = entitlements)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onSubscribeClicked()
            advanceUntilIdle()

            viewModel.onSubscriptionPurchaseCompleted(
                SubscriptionPurchaseOutcome.Pending(googlePlayPurchase(state = GooglePlayPurchaseState.Pending)),
            )
            advanceUntilIdle()

            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertFalse(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertFalse(effects.any { it is SettingsUiEffect.AcknowledgeGooglePlayPurchase })
            assertEquals(0, entitlements.validateCallCount)
            assertNull(entitlements.lastValidatedPurchase)
            assertNull(viewModel.uiState.value.feedback)
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }


    @Test
    fun `restore activo emite refresh economy`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val subscriptionRepo = FakeSubscriptionRepository(
                restoreResult = RestorePurchasesResult.Restored(SubscriptionStatus.ActiveMonthly),
            )
            val viewModel = viewModel(analytics = analytics, subscriptionRepo = subscriptionRepo)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertEquals(1, subscriptionRepo.restoreCallCount)
            assertEquals(SettingsFeedback.RestorePurchasesSuccess, viewModel.uiState.value.feedback)
            assertTrue(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreClicked })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreCompleted })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumRestoreEmpty })
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `restore sin entitlement activo no emite refresh economy`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val subscriptionRepo = FakeSubscriptionRepository(restoreResult = RestorePurchasesResult.NoPurchasesFound)
            val viewModel = viewModel(analytics = analytics, subscriptionRepo = subscriptionRepo)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertEquals(1, subscriptionRepo.restoreCallCount)
            assertEquals(SettingsFeedback.RestorePurchasesNoPurchases, viewModel.uiState.value.feedback)
            assertFalse(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreClicked })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreEmpty })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumRestoreCompleted })
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `restore no purchases pero backend activo devuelve success y refresh economy`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val subscriptionRepo = FakeSubscriptionRepository(restoreResult = RestorePurchasesResult.NoPurchasesFound)
            val entitlements = FakePremiumEntitlementRepository(
                refreshEntitlement = PremiumEntitlement(isActive = true, status = SubscriptionStatus.ActiveMonthly),
            )
            val viewModel = viewModel(analytics = analytics, subscriptionRepo = subscriptionRepo, entitlements = entitlements)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertEquals(SettingsFeedback.RestorePurchasesSuccess, viewModel.uiState.value.feedback)
            assertEquals(SubscriptionStatus.ActiveMonthly, viewModel.uiState.value.subscriptionStatus)
            assertTrue(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreCompleted })
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `restore inactive no emite completed aunque el resultado sea restored`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val subscriptionRepo = FakeSubscriptionRepository(
                restoreResult = RestorePurchasesResult.Restored(SubscriptionStatus.Inactive),
            )
            val viewModel = viewModel(analytics = analytics, subscriptionRepo = subscriptionRepo)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertEquals(1, subscriptionRepo.restoreCallCount)
            assertEquals(SettingsFeedback.RestorePurchasesNoPurchases, viewModel.uiState.value.feedback)
            assertFalse(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreClicked })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreEmpty })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumRestoreCompleted })
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `init con backend entitlement active marca ActiveMonthly y Manage`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val entitlements = FakePremiumEntitlementRepository(
                refreshEntitlement = PremiumEntitlement(isActive = true, status = SubscriptionStatus.ActiveMonthly),
            )
            val subscriptionRepo = FakeSubscriptionRepository()
            val viewModel = viewModel(
                analytics = analytics,
                entitlements = entitlements,
                subscriptionRepo = subscriptionRepo.copyWithStatus(SubscriptionStatus.ActiveMonthly),
                authUser = AuthUser(uid = "user-1", email = "user@example.com", isAnonymous = false),
            )

            advanceUntilIdle()

            assertEquals(1, entitlements.refreshCallCount)
            assertEquals(0, subscriptionRepo.restoreCallCount)
            assertEquals(SubscriptionStatus.ActiveMonthly, viewModel.uiState.value.subscriptionStatus)
            assertEquals(SubscriptionPrimaryAction.Manage, viewModel.uiState.value.subscriptionPrimaryAction)
            assertTrue(analytics.events.any { it is AnalyticsEvent.EntitlementRefreshed })
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `init con backend entitlement inactive mantiene Inactive y Subscribe`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val entitlements = FakePremiumEntitlementRepository(
                refreshEntitlement = PremiumEntitlement(isActive = false, status = SubscriptionStatus.Inactive),
            )
            val subscriptionRepo = FakeSubscriptionRepository()
            val viewModel = viewModel(
                entitlements = entitlements,
                subscriptionRepo = subscriptionRepo,
                authUser = AuthUser(uid = "user-1", email = "user@example.com", isAnonymous = false),
            )

            advanceUntilIdle()

            assertEquals(1, entitlements.refreshCallCount)
            assertEquals(0, subscriptionRepo.restoreCallCount)
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
            assertEquals(SubscriptionPrimaryAction.Subscribe, viewModel.uiState.value.subscriptionPrimaryAction)
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `fallo de refresh en init no rompe UI ni llama restore`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val entitlements = FakePremiumEntitlementRepository(
                refreshError = IllegalStateException("backend failed"),
            )
            val subscriptionRepo = FakeSubscriptionRepository()
            val viewModel = viewModel(
                analytics = analytics,
                entitlements = entitlements,
                subscriptionRepo = subscriptionRepo,
                authUser = AuthUser(uid = "user-1", email = "user@example.com", isAnonymous = false),
            )

            advanceUntilIdle()

            assertEquals(1, entitlements.refreshCallCount)
            assertEquals(0, subscriptionRepo.restoreCallCount)
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
            assertEquals(SubscriptionPrimaryAction.Subscribe, viewModel.uiState.value.subscriptionPrimaryAction)
            assertFalse(viewModel.uiState.value.isLoading)
            assertNotNull(viewModel.uiState.value.error)
            assertTrue(analytics.events.any { it is AnalyticsEvent.EntitlementRefreshFailed })
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `annual selection is not purchasable`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = viewModel(analytics = analytics)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onSubscribeClicked(SubscriptionPlanSelection.Annual)
            advanceUntilIdle()

            assertFalse(effects.any { it is SettingsUiEffect.LaunchSubscriptionPurchase })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseStarted })
            collectJob.cancel()
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `catalogo de settings renderiza solo mensual`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = viewModel(
                subscriptionRepo = FakeSubscriptionRepository(
                    catalog = listOf(
                        SubscriptionPlan("bwitch_premium_monthly", "Monthly", "$4.99", SubscriptionPlanType.Monthly, "monthly"),
                        SubscriptionPlan("reserved_annual", "Annual", "$49.99", SubscriptionPlanType.Annual, "annual"),
                    ),
                ),
            )

            advanceUntilIdle()

            assertEquals(listOf("bwitch_premium_monthly"), viewModel.uiState.value.subscriptionCatalog.map { it.productId })
            viewModel.clear()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `known products usan product id mensual real y solo consultan mensual`() {
        assertEquals("bwitch_premium_monthly", KnownSubscriptionProducts.MONTHLY)
        assertEquals(listOf("bwitch_premium_monthly"), KnownSubscriptionProducts.ordered)
        assertEquals(setOf("bwitch_premium_monthly"), KnownSubscriptionProducts.all)
    }

    private fun viewModel(
        analytics: FakeAnalyticsTracker = FakeAnalyticsTracker(),
        entitlements: FakePremiumEntitlementRepository = FakePremiumEntitlementRepository(),
        subscriptionRepo: FakeSubscriptionRepository = FakeSubscriptionRepository(),
        authUser: AuthUser? = null,
    ): SettingsViewModel {
        val notificationRepo = FakeNotificationSettingsRepository()
        val profileRepo = FakeUserProfileRepository()
        val pushRepo = FakePushRegistrationRepository()
        val testPushRepo = FakePushTestNotificationRepository()
        return SettingsViewModel(
            observeUserProfile = ObserveUserProfileUseCase(profileRepo),
            getUserProfile = GetUserProfileUseCase(profileRepo),
            sessionViewModel = SessionViewModel(FakeAuthRepository(authUser)),
            observeCurrentLanguage = ObserveCurrentLanguageUseCase(FakeLanguageRepository()),
            observeNotificationSettings = ObserveNotificationSettingsUseCase(notificationRepo),
            getNotificationSettings = GetNotificationSettingsUseCase(notificationRepo),
            updateNotificationSettings = UpdateNotificationSettingsUseCase(notificationRepo),
            observeSubscriptionStatus = ObserveSubscriptionStatusUseCase(subscriptionRepo),
            getSubscriptionStatus = GetSubscriptionStatusUseCase(subscriptionRepo),
            getSubscriptionCatalog = GetSubscriptionCatalogUseCase(subscriptionRepo),
            restorePurchases = RestorePurchasesUseCase(subscriptionRepo),
            refreshPremiumEntitlement = RefreshPremiumEntitlementUseCase(entitlements),
            validateGooglePlayPurchase = ValidateGooglePlayPurchaseUseCase(entitlements),
            registerPushToken = RegisterPushTokenUseCase(pushRepo),
            updatePushNotificationPreferences = UpdatePushNotificationPreferencesUseCase(pushRepo),
            sendTestNotification = SendTestNotificationUseCase(testPushRepo),
            analyticsTracker = analytics,
        )
    }

    private class FakeSubscriptionRepository(
        private val catalog: List<SubscriptionPlan> = listOf(
            SubscriptionPlan("bwitch_premium_monthly", "Monthly", "$4.99", SubscriptionPlanType.Monthly, "monthly"),
        ),
        private val restoreResult: RestorePurchasesResult = RestorePurchasesResult.NoPurchasesFound,
        initialStatus: SubscriptionStatus = SubscriptionStatus.Inactive,
    ) : SubscriptionRepository {
        private val status = MutableStateFlow(initialStatus)

        override suspend fun getStatus(): SubscriptionStatus = status.value
        override suspend fun getCatalog(): List<SubscriptionPlan> = catalog
        var restoreCallCount: Int = 0

        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override suspend fun restorePurchases(): RestorePurchasesResult {
            restoreCallCount += 1
            return restoreResult
        }

        fun copyWithStatus(status: SubscriptionStatus): FakeSubscriptionRepository = FakeSubscriptionRepository(
            catalog = catalog,
            restoreResult = restoreResult,
            initialStatus = status,
        )
    }

    private class FakePremiumEntitlementRepository(
        private val validateEntitlement: PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive),
        private val refreshEntitlement: PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive),
        private val refreshError: Throwable? = null,
    ) : PremiumEntitlementRepository {
        var lastValidatedPurchase: GooglePlayPurchase? = null
        var validateCallCount: Int = 0
        var refreshCallCount: Int = 0

        override suspend fun validateGooglePlayPurchase(purchase: GooglePlayPurchase): PremiumEntitlement {
            validateCallCount += 1
            lastValidatedPurchase = purchase
            return validateEntitlement
        }

        override suspend fun restoreGooglePlayPurchases(purchases: List<GooglePlayPurchase>): PremiumEntitlement =
            PremiumEntitlement(false, SubscriptionStatus.Inactive)

        override suspend fun refreshEntitlement(): PremiumEntitlement {
            refreshCallCount += 1
            refreshError?.let { throw it }
            return refreshEntitlement
        }
    }

    private class FakeNotificationSettingsRepository : NotificationSettingsRepository {
        private val state = MutableStateFlow(NotificationSettings())
        override suspend fun get(): NotificationSettings = state.value
        override fun observe(): Flow<NotificationSettings> = state
        override suspend fun update(settings: NotificationSettings) {
            state.value = settings
        }
    }


    private class FakePushRegistrationRepository : PushRegistrationRepository {
        override suspend fun registerToken(payload: PushTokenRegistration) = Unit
        override suspend fun unregisterToken(token: String, platform: PushPlatform) = Unit
        override suspend fun updatePreferences(preferences: PushNotificationPreferences) = Unit
    }
    private class FakePushTestNotificationRepository : PushTestNotificationRepository {
        override suspend fun sendTestNotification() = Unit
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        override fun observeUserProfile(): Flow<UserProfile?> = MutableStateFlow(null)
        override suspend fun getUserProfile(): UserProfile? = null
        override suspend fun saveUserProfile(profile: UserProfile) = Unit
    }

    private class FakeLanguageRepository : AppLanguageRepository {
        override suspend fun resolveCurrentLanguage(): AppLanguage = AppLanguage.English
        override suspend fun getCurrentLanguage(): AppLanguage = AppLanguage.English
        override suspend fun setCurrentLanguage(language: AppLanguage) = Unit
        override fun observeCurrentLanguage(): Flow<AppLanguage> = MutableStateFlow(AppLanguage.English)
    }

    private class FakeAuthRepository(user: AuthUser?) : AuthRepository {
        override val authState: Flow<AuthUser?> = MutableStateFlow(user)
        override suspend fun signInWithEmail(email: String, password: String) = Unit
        override suspend fun signUpWithEmail(email: String, password: String) = Unit
        override suspend fun signOut() = Unit
        override suspend fun signInWithGoogleIdToken(idToken: String) = Unit
    }
}

private fun googlePlayPurchase(
    state: GooglePlayPurchaseState = GooglePlayPurchaseState.Purchased,
): GooglePlayPurchase = GooglePlayPurchase(
    productId = KnownSubscriptionProducts.MONTHLY,
    purchaseToken = "token-123",
    purchaseState = state,
    isAcknowledged = false,
    orderId = "order-123",
    packageName = "com.agc.bwitch",
)
