package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionCatalogUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.NotificationSettingsRepository
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelAnalyticsTest {

    @Test
    fun `premium CTA click and purchase success emit analytics`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val subscriptionRepo = FakeSubscriptionRepository()
            val notificationRepo = FakeNotificationSettingsRepository()
            val analytics = FakeAnalyticsTracker()
            val profileRepo = FakeUserProfileRepository()
            val viewModel = SettingsViewModel(
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
                analyticsTracker = analytics,
            )

            advanceUntilIdle()
            viewModel.onSubscriptionPrimaryActionClicked()
            viewModel.onSubscriptionPurchaseCompleted(SubscriptionPurchaseOutcome.Success)
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumCtaClicked })
            assertTrue(analytics.events.any { it is AnalyticsEvent.PremiumPurchaseCompleted })
            assertTrue(analytics.events.none { it is AnalyticsEvent.PremiumCtaShown })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeSubscriptionRepository : SubscriptionRepository {
        private val status = MutableStateFlow<SubscriptionStatus>(SubscriptionStatus.Inactive)

        override suspend fun getStatus(): SubscriptionStatus = status.value
        override suspend fun getCatalog(): List<SubscriptionPlan> = listOf(
            SubscriptionPlan("premium.monthly", "Monthly", "$4.99", SubscriptionPlanType.Monthly),
        )
        override fun observeStatus(): Flow<SubscriptionStatus> = status
        override suspend fun restorePurchases(): RestorePurchasesResult = RestorePurchasesResult.NoPurchasesFound
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
