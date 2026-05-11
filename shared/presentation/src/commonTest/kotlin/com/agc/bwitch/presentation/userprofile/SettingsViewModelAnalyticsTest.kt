package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.settings.BillingPurchaseToken
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionCatalogUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.PurchaseState
import com.agc.bwitch.domain.settings.PremiumEntitlement
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.PremiumRestoreResult
import com.agc.bwitch.domain.settings.PremiumSubscriptionStatus
import com.agc.bwitch.domain.settings.NotificationSettingsRepository
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
import com.agc.bwitch.domain.settings.RefreshPremiumEntitlementUseCase
import com.agc.bwitch.domain.settings.RestoreGooglePlayPurchasesUseCase
import com.agc.bwitch.domain.settings.ValidateGooglePlayPurchaseUseCase
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelAnalyticsTest {

    @Test
    fun `purchased token plus backend active emits completed and marks premium active`() = runTest {
        withViewModel(
            premiumRepo = FakePremiumEntitlementRepository(
                validateEntitlement = activeEntitlement(),
            ),
        ) { viewModel, analytics, _, _ ->
            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(testPurchaseToken()))
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchasePending })
            assertEquals(SubscriptionStatus.ActiveMonthly, viewModel.uiState.value.subscriptionStatus)
        }
    }

    @Test
    fun `purchased token plus backend pending emits pending and does not mark premium active`() = runTest {
        withViewModel(
            premiumRepo = FakePremiumEntitlementRepository(
                validateEntitlement = pendingEntitlement(),
            ),
        ) { viewModel, analytics, _, _ ->
            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(testPurchaseToken()))
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchasePending })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
        }
    }

    @Test
    fun `pending billing purchase emits pending without completed analytics`() = runTest {
        withViewModel { viewModel, analytics, _, premiumRepo ->
            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onSubscriptionPurchaseCompleted(
                SubscriptionPurchaseOutcome.Pending(testPurchaseToken(purchaseState = PurchaseState.Pending)),
            )
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchasePending })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertEquals(0, premiumRepo.validateCalls)
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
        }
    }

    @Test
    fun `backend validation failure emits failed analytics`() = runTest {
        withViewModel(
            premiumRepo = FakePremiumEntitlementRepository(
                validateError = IllegalStateException("validation failed"),
            ),
        ) { viewModel, analytics, _, _ ->
            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(testPurchaseToken()))
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseFailed })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
        }
    }

    @Test
    fun `restore tokens plus active entitlement emits restore completed and marks premium active`() = runTest {
        withViewModel(
            subscriptionRepo = FakeSubscriptionRepository(
                restoreResult = RestorePurchasesResult.RestorableTokens(listOf(testPurchaseToken())),
            ),
            premiumRepo = FakePremiumEntitlementRepository(
                restoreResult = PremiumRestoreResult(
                    entitlement = activeEntitlement(),
                    restoredCount = 1,
                    activeTokenFound = true,
                ),
            ),
        ) { viewModel, analytics, _, _ ->
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreCompleted })
            assertEquals(SubscriptionStatus.ActiveMonthly, viewModel.uiState.value.subscriptionStatus)
        }
    }

    @Test
    fun `restore tokens plus no active entitlement emits restore empty and does not mark premium active`() = runTest {
        withViewModel(
            subscriptionRepo = FakeSubscriptionRepository(
                restoreResult = RestorePurchasesResult.RestorableTokens(listOf(testPurchaseToken())),
            ),
            premiumRepo = FakePremiumEntitlementRepository(
                restoreResult = PremiumRestoreResult(
                    entitlement = inactiveEntitlement(),
                    restoredCount = 1,
                    activeTokenFound = false,
                ),
            ),
        ) { viewModel, analytics, _, _ ->
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreEmpty })
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumRestoreCompleted })
            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
        }
    }

    @Test
    fun `no local restore tokens emits restore empty`() = runTest {
        withViewModel { viewModel, analytics, _, premiumRepo ->
            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumRestoreEmpty })
            assertEquals(0, premiumRepo.restoreCalls)
        }
    }


    @Test
    fun `active backend purchase requests economy refresh effect`() = runTest {
        withViewModel(
            premiumRepo = FakePremiumEntitlementRepository(
                validateEntitlement = activeEntitlement(),
            ),
        ) { viewModel, _, _, _ ->
            val effects = mutableListOf<SettingsUiEffect>()
            val job = launch { viewModel.uiEffects.collect { effects.add(it) } }
            advanceUntilIdle()

            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Purchased(testPurchaseToken()))
            advanceUntilIdle()

            assertTrue(effects.any { it is SettingsUiEffect.RefreshEconomySnapshot })
            job.cancel()
        }
    }

    @Test
    fun `active backend restore requests economy refresh effect`() = runTest {
        withViewModel(
            subscriptionRepo = FakeSubscriptionRepository(
                restoreResult = RestorePurchasesResult.RestorableTokens(listOf(testPurchaseToken())),
            ),
            premiumRepo = FakePremiumEntitlementRepository(
                restoreResult = PremiumRestoreResult(
                    entitlement = activeEntitlement(),
                    restoredCount = 1,
                    activeTokenFound = true,
                ),
            ),
        ) { viewModel, _, _, _ ->
            val effects = mutableListOf<SettingsUiEffect>()
            val job = launch { viewModel.uiEffects.collect { effects.add(it) } }
            advanceUntilIdle()

            viewModel.onRestorePurchasesClicked()
            advanceUntilIdle()

            assertTrue(effects.any { it is SettingsUiEffect.RefreshEconomySnapshot })
            job.cancel()
        }
    }

    @Test
    fun `premium paywall shown analytics is explicit and not emitted by init`() = runTest {
        withViewModel { viewModel, analytics, _, _ ->
            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPaywallShown })

            viewModel.onPremiumPaywallShown(placement = "settings", originPlacement = "settings")
            advanceUntilIdle()

            assertEquals(1, analytics.events.count { it is AnalyticsEvent.PremiumPaywallShown })
        }
    }


    @Test
    fun `premium paywall shown is ignored when backend entitlement is active`() = runTest {
        withViewModel(
            premiumRepo = FakePremiumEntitlementRepository(
                refreshEntitlement = activeEntitlement(),
            ),
        ) { viewModel, analytics, _, _ ->
            viewModel.onPremiumPaywallShown(placement = "settings", originPlacement = "settings")
            advanceUntilIdle()

            assertFalse(analytics.events.any { it is AnalyticsEvent.PremiumPaywallShown })
        }
    }

    @Test
    fun `local billing active status never directly sets premium`() = runTest {
        withViewModel(
            subscriptionRepo = FakeSubscriptionRepository(initialStatus = SubscriptionStatus.ActiveMonthly),
            premiumRepo = FakePremiumEntitlementRepository(refreshEntitlement = inactiveEntitlement()),
        ) { viewModel, _, _, _ ->
            advanceUntilIdle()

            assertEquals(SubscriptionStatus.Inactive, viewModel.uiState.value.subscriptionStatus)
        }
    }

    private suspend fun TestScope.withViewModel(
        subscriptionRepo: FakeSubscriptionRepository = FakeSubscriptionRepository(),
        premiumRepo: FakePremiumEntitlementRepository = FakePremiumEntitlementRepository(),
        block: suspend TestScope.(SettingsViewModel, FakeAnalyticsTracker, FakeSubscriptionRepository, FakePremiumEntitlementRepository) -> Unit,
    ) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val notificationRepo = FakeNotificationSettingsRepository()
            val userProfileRepository = FakeUserProfileRepository()
            val analytics = FakeAnalyticsTracker()
            val viewModel = SettingsViewModel(
                observeUserProfile = ObserveUserProfileUseCase(userProfileRepository),
                getUserProfile = GetUserProfileUseCase(userProfileRepository),
                sessionViewModel = SessionViewModel(FakeAuthRepository()),
                observeCurrentLanguage = ObserveCurrentLanguageUseCase(FakeLanguageRepository()),
                observeNotificationSettings = ObserveNotificationSettingsUseCase(notificationRepo),
                getNotificationSettings = GetNotificationSettingsUseCase(notificationRepo),
                updateNotificationSettings = UpdateNotificationSettingsUseCase(notificationRepo),
                observeSubscriptionStatus = ObserveSubscriptionStatusUseCase(subscriptionRepo),
                getSubscriptionStatus = GetSubscriptionStatusUseCase(subscriptionRepo),
                getSubscriptionCatalog = GetSubscriptionCatalogUseCase(subscriptionRepo),
                restorePurchases = RestorePurchasesUseCase(subscriptionRepo),
                refreshPremiumEntitlement = RefreshPremiumEntitlementUseCase(premiumRepo),
                validateGooglePlayPurchase = ValidateGooglePlayPurchaseUseCase(premiumRepo),
                restoreGooglePlayPurchases = RestoreGooglePlayPurchasesUseCase(premiumRepo),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()
            block(viewModel, analytics, subscriptionRepo, premiumRepo)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun testPurchaseToken(
        purchaseState: PurchaseState = PurchaseState.Purchased,
    ): BillingPurchaseToken = BillingPurchaseToken(
        productId = KnownSubscriptionProducts.MONTHLY,
        purchaseToken = "purchase-token",
        purchaseState = purchaseState,
        acknowledged = false,
        packageName = "com.bwitch.app",
    )

    private fun activeEntitlement(): PremiumEntitlement = PremiumEntitlement(
        isSubscriber = true,
        status = PremiumSubscriptionStatus.Active,
        productId = KnownSubscriptionProducts.MONTHLY,
        platform = "google_play",
        environment = "test",
    )

    private fun pendingEntitlement(): PremiumEntitlement = PremiumEntitlement(
        isSubscriber = false,
        status = PremiumSubscriptionStatus.Pending,
        productId = KnownSubscriptionProducts.MONTHLY,
        platform = "google_play",
        environment = "test",
    )

    private fun inactiveEntitlement(): PremiumEntitlement = PremiumEntitlement(
        isSubscriber = false,
        status = PremiumSubscriptionStatus.None,
    )

    private class FakeSubscriptionRepository(
        initialStatus: SubscriptionStatus = SubscriptionStatus.Inactive,
        private val restoreResult: RestorePurchasesResult = RestorePurchasesResult.NoPurchasesFound,
    ) : SubscriptionRepository {
        private val status = MutableStateFlow(initialStatus)

        override suspend fun getStatus(): SubscriptionStatus = status.value
        override suspend fun getCatalog(): List<SubscriptionPlan> = listOf(
            SubscriptionPlan(KnownSubscriptionProducts.MONTHLY, "Monthly", "$4.99", SubscriptionPlanType.Monthly),
        )
        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override suspend fun restorePurchases(): RestorePurchasesResult = restoreResult
    }

    private class FakePremiumEntitlementRepository(
        private val refreshEntitlement: PremiumEntitlement = defaultInactiveEntitlement(),
        private val validateEntitlement: PremiumEntitlement = defaultInactiveEntitlement(),
        private val validateError: Throwable? = null,
        private val restoreResult: PremiumRestoreResult = PremiumRestoreResult(defaultInactiveEntitlement(), 0, false),
    ) : PremiumEntitlementRepository {
        var validateCalls = 0
        var restoreCalls = 0

        override suspend fun refreshPremiumEntitlement(force: Boolean): PremiumEntitlement = refreshEntitlement

        override suspend fun validateGooglePlayPurchase(token: BillingPurchaseToken): PremiumEntitlement {
            validateCalls += 1
            validateError?.let { throw it }
            return validateEntitlement
        }

        override suspend fun restoreGooglePlayPurchases(tokens: List<BillingPurchaseToken>): PremiumRestoreResult {
            restoreCalls += 1
            return restoreResult
        }

        companion object {
            private fun defaultInactiveEntitlement(): PremiumEntitlement = PremiumEntitlement(
                isSubscriber = false,
                status = PremiumSubscriptionStatus.None,
            )
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
