package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
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
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
            val collectJob = backgroundScope.launch { viewModel.uiEffects.collect { effects += it } }

            advanceUntilIdle()
            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onPremiumCtaShown("settings_subscribe")
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(googlePlayPurchase()))
            advanceUntilIdle()

            val premiumClickedEvents = analytics.events.filterIsInstance<AnalyticsEvent.PremiumCtaClicked>()
            val premiumShownEvents = analytics.events.filterIsInstance<AnalyticsEvent.PremiumCtaShown>()
            val premiumStartedEvents = analytics.events.filterIsInstance<AnalyticsEvent.PremiumPurchaseStarted>()

            assertEquals("token-123", entitlements.lastValidatedPurchase?.purchaseToken)
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertTrue(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertTrue(effects.any { it is SettingsUiEffect.AcknowledgeGooglePlayPurchase })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumCtaClicked })
            assertTrue(premiumShownEvents.isNotEmpty())
            assertTrue(premiumStartedEvents.isNotEmpty())
            assertTrue(premiumClickedEvents.all { it.originPlacement == "settings" })
            assertTrue(premiumShownEvents.all { it.originPlacement == "settings" })
            assertTrue(premiumStartedEvents.all { it.originPlacement == "settings" })
            assertEquals("settings", premiumClickedEvents.first().originPlacement)
            collectJob.cancel()
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
            val viewModel = viewModel(analytics = analytics)
            val effects = mutableListOf<SettingsUiEffect>()
            val collectJob = backgroundScope.launch { viewModel.uiEffects.collect { effects += it } }

            advanceUntilIdle()
            viewModel.onSubscribeClicked()
            viewModel.onSubscriptionPurchaseCompleted(
                SubscriptionPurchaseOutcome.Pending(googlePlayPurchase(state = GooglePlayPurchaseState.Pending)),
            )
            advanceUntilIdle()

            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertFalse(effects.any { it is SettingsUiEffect.RefreshEconomy })
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
            collectJob.cancel()
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
            val collectJob = backgroundScope.launch { viewModel.uiEffects.collect { effects += it } }

            advanceUntilIdle()
            viewModel.onSubscribeClicked(SubscriptionPlanSelection.Annual)
            advanceUntilIdle()

            assertFalse(effects.any { it is SettingsUiEffect.LaunchSubscriptionPurchase })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseStarted })
            collectJob.cancel()
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
    ): SettingsViewModel {
        val notificationRepo = FakeNotificationSettingsRepository()
        val profileRepo = FakeUserProfileRepository()
        return SettingsViewModel(
            observeUserProfile = ObserveUserProfileUseCase(profileRepo),
            getUserProfile = GetUserProfileUseCase(profileRepo),
            sessionViewModel = SessionViewModel(FakeAuthRepository()),
            observeCurrentLanguage = ObserveCurrentLanguageUseCase(FakeLanguageRepository()),
            observeNotificationSettings = ObserveNotificationSettingsUseCase(notificationRepo),
            getNotificationSettings = GetNotificationSettingsUseCase(notificationRepo),
            updateNotificationSettings = UpdateNotificationSettingsUseCase(notificationRepo),
            observeSubscriptionStatus = ObserveSubscriptionStatusUseCase(subscriptionRepo),
            getSubscriptionStatus = GetSubscriptionStatusUseCase(subscriptionRepo),
            getSubscriptionCatalog = GetSubscriptionCatalogUseCase(subscriptionRepo),
            restorePurchases = RestorePurchasesUseCase(subscriptionRepo),
            validateGooglePlayPurchase = ValidateGooglePlayPurchaseUseCase(entitlements),
            analyticsTracker = analytics,
        )
    }

    private class FakeSubscriptionRepository(
        private val catalog: List<SubscriptionPlan> = listOf(
            SubscriptionPlan("bwitch_premium_monthly", "Monthly", "$4.99", SubscriptionPlanType.Monthly, "monthly"),
        ),
    ) : SubscriptionRepository {
        private val status = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)

        override suspend fun getStatus(): SubscriptionStatus = status.value
        override suspend fun getCatalog(): List<SubscriptionPlan> = catalog
        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override suspend fun restorePurchases(): RestorePurchasesResult = RestorePurchasesResult.NoPurchasesFound
    }

    private class FakePremiumEntitlementRepository(
        private val validateEntitlement: PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive),
    ) : PremiumEntitlementRepository {
        var lastValidatedPurchase: GooglePlayPurchase? = null

        override suspend fun validateGooglePlayPurchase(purchase: GooglePlayPurchase): PremiumEntitlement {
            lastValidatedPurchase = purchase
            return validateEntitlement
        }

        override suspend fun restoreGooglePlayPurchases(purchases: List<GooglePlayPurchase>): PremiumEntitlement =
            PremiumEntitlement(false, SubscriptionStatus.Inactive)

        override suspend fun refreshEntitlement(): PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive)
    }

    private class FakeNotificationSettingsRepository : NotificationSettingsRepository {
        private val state = MutableStateFlow(NotificationSettings())
        override suspend fun get(): NotificationSettings = state.value
        override fun observe(): Flow<NotificationSettings> = state
        override suspend fun update(settings: NotificationSettings) {
            state.value = settings
        }
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

    private class FakeAuthRepository : AuthRepository {
        override val authState: Flow<com.agc.bwitch.domain.auth.AuthUser?> = emptyFlow()
        override suspend fun signInWithEmail(email: String, password: String) = Unit
        override suspend fun signUpWithEmail(email: String, password: String) = Unit
        override suspend fun signOut() = Unit
        override suspend fun signInWithGoogleIdToken(idToken: String) = Unit
    }
}

private fun googlePlayPurchase(
    state: GooglePlayPurchaseState = GooglePlayPurchaseState.Purchased,
): GooglePlayPurchase = GooglePlayPurchase(
    productId = "bwitch_premium_monthly",
    purchaseToken = "token-123",
    purchaseState = state,
    isAcknowledged = false,
    orderId = "order-123",
    packageName = "com.agc.bwitch",
)
