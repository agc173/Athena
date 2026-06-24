package com.agc.bwitch.data.di

import com.agc.bwitch.data.account.FunctionsAccountDeletionRepository
import com.agc.bwitch.data.astrology.birthchart.SettingsBirthChartRepository
import com.agc.bwitch.data.astrology.birthchart.SyncBirthChartRepository
import com.agc.bwitch.data.auth.FirebaseAuthRepository
import com.agc.bwitch.data.economy.EconomyRepositoryImpl
import com.agc.bwitch.data.connectivity.AlwaysOnlineConnectivityChecker
import com.agc.bwitch.data.connectivity.ConnectivityChecker
import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.data.functions.GitLiveFunctionsClient
import com.agc.bwitch.data.oracle.OracleRepositoryImpl
import com.agc.bwitch.data.remote.economy.EconomyRemoteDataSource
import com.agc.bwitch.data.localization.SettingsAppLanguageRepository
import com.agc.bwitch.data.localization.SystemLanguageCodeProvider
import com.agc.bwitch.data.moons.BackendFirstMoonRepository
import com.agc.bwitch.data.moons.BillingBackedMoonPackRepository
import com.agc.bwitch.data.moons.FunctionsMoonPackPurchaseRepository
import com.agc.bwitch.data.moons.MockMoonPackRepository
import com.agc.bwitch.data.moons.MoonPackBillingDataSource
import com.agc.bwitch.data.moons.UnsupportedMoonPackBillingDataSource
import com.agc.bwitch.data.notifications.FunctionsPushRegistrationRepository
import com.agc.bwitch.data.moons.SettingsMoonRepository
import com.agc.bwitch.data.rituals.LocalRitualCatalogRepository
import com.agc.bwitch.data.rituals.SettingsDailyRitualRepository
import com.agc.bwitch.data.rituals.SyncDailyRitualRepository
import com.agc.bwitch.data.rituals.FirestoreBackedRitualCatalogRepository
import com.agc.bwitch.data.rituals.SettingsHabitsRepository
import com.agc.bwitch.data.rituals.SyncHabitsRepository
import com.agc.bwitch.data.session.LocalUserDataRepositoryImpl
import com.agc.bwitch.data.settings.SettingsNotificationSettingsRepository
import com.agc.bwitch.data.settings.BillingBackedSubscriptionRepository
import com.agc.bwitch.data.settings.entitlement.FunctionsPremiumEntitlementRepository
import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.data.settings.billing.UnsupportedSubscriptionBillingDataSource
import com.agc.bwitch.data.tarot.TarotRepositoryImpl
import com.agc.bwitch.data.tarot.FirestoreTarotDeckCollectionRepository
import com.agc.bwitch.data.tarot.SettingsLastTarotReadingRepository
import com.agc.bwitch.data.tarot.SettingsSelectedTarotDeckRepository
import com.agc.bwitch.data.userprofile.FirebaseAvatarRepository
import com.agc.bwitch.data.userprofile.SettingsUserProfileRepository
import com.agc.bwitch.data.userprofile.SyncUserProfileRepository
import com.agc.bwitch.domain.account.AccountDeletionRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockRepository
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.moons.MoonPackRepository
import com.agc.bwitch.domain.moons.MoonPackPurchaseRepository
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.notifications.PushRegistrationRepository
import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.rituals.HabitsRepository
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.domain.session.LocalUserDataRepository
import com.agc.bwitch.domain.settings.NotificationSettingsRepository
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotDeckCollectionRepository
import com.agc.bwitch.domain.tarot.LastTarotReadingRepository
import com.agc.bwitch.domain.tarot.SelectedTarotDeckRepository
import com.agc.bwitch.domain.userprofile.AvatarRepository
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.domain.astrology.birthchart.BirthChartSyncController
import com.agc.bwitch.domain.userprofile.UserProfileSyncController
import com.agc.bwitch.data.astrology.horoscope.SettingsHoroscopeDailyRepository
import com.agc.bwitch.data.astrology.horoscope.SyncHoroscopeDailyRepository
import com.agc.bwitch.data.astrology.horoscope.SyncHoroscopeUnlockRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.data.astrology.horoscope.SettingsHoroscopePullMarkerRepository
import com.agc.bwitch.data.astrology.horoscope.SettingsConstellationProgressRepository
import com.agc.bwitch.data.astrology.horoscope.BackendFirstConstellationProgressRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

val dataKoinModule: Module = module {
    /**
     * Localization
     */
    single { SystemLanguageCodeProvider() }
    single<AppLanguageRepository> { SettingsAppLanguageRepository(get(), get()) }

    single<NotificationSettingsRepository> { SettingsNotificationSettingsRepository(get()) }
    single<SubscriptionBillingDataSource> { UnsupportedSubscriptionBillingDataSource }
    single<PremiumEntitlementRepository> { FunctionsPremiumEntitlementRepository(get()) }
    single<SubscriptionRepository> { BillingBackedSubscriptionRepository(get(), get(), get()) }
    single { SettingsMoonRepository(get()) }
    single<MoonRepository> { BackendFirstMoonRepository(localRepository = get<SettingsMoonRepository>(), economyRepository = get()) }
    single<MoonPackBillingDataSource> { UnsupportedMoonPackBillingDataSource }
    single<MoonPackRepository> { BillingBackedMoonPackRepository(get()) }
    single<MoonPackPurchaseRepository> { FunctionsMoonPackPurchaseRepository(get()) }
    single { FunctionsPushRegistrationRepository(get()) }
    single<PushRegistrationRepository> { get<FunctionsPushRegistrationRepository>() }

    /**
     * Auth
     */
    single<AuthRepository> { FirebaseAuthRepository() }
    single<AccountDeletionRepository> { FunctionsAccountDeletionRepository(get(), get()) }

    /**
     * Horoscope
     */
    single { SettingsHoroscopeDailyRepository(get()) }

    // ✅ Registra la implementación concreta (con authRepository)
    single {
        SyncHoroscopeDailyRepository(
            local = get(),
            authRepository = get()
        )
    }

    // ✅ Expón la misma instancia como interfaces
    single<HoroscopeRepository> { get<SyncHoroscopeDailyRepository>() }
    single<HoroscopeDailySyncController> { get<SyncHoroscopeDailyRepository>() }

    single<HoroscopePullMarker> { SettingsHoroscopePullMarkerRepository(get()) }
    single<HoroscopeUnlockRepository> { SyncHoroscopeUnlockRepository(get()) }
    single { SettingsConstellationProgressRepository(get()) }
    single<ConstellationProgressRepository> { BackendFirstConstellationProgressRepository(localRepository = get(), authRepository = get(), functionsClient = get()) }

    /**
     * BirthChart - LOCAL
     */
    single { SettingsBirthChartRepository(get()) }

    /**
     * BirthChart - SYNC (source of truth del dominio)
     */
    single { SyncBirthChartRepository(get(), get(), get(), get()) }
    single<BirthChartRepository> { get<SyncBirthChartRepository>() }
    single<BirthChartSyncController> { get<SyncBirthChartRepository>() }

    /**
     * UserProfile - LOCAL
     */
    single { SettingsUserProfileRepository(get()) }

    /**
     * UserProfile - SYNC (source of truth del dominio)
     */
    single { SyncUserProfileRepository(get(), get(), get()) }
    single<UserProfileRepository> { get<SyncUserProfileRepository>() }
    single<UserProfileSyncController> { get<SyncUserProfileRepository>() }

    /**
     * Avatar upload (Storage)
     */
    single<AvatarRepository> { FirebaseAvatarRepository(get()) }


    /**
     * Cloud Functions
     */
    single<ConnectivityChecker> { AlwaysOnlineConnectivityChecker }
    single<FunctionsClient> { GitLiveFunctionsClient(connectivityChecker = get()) }
    single { EconomyRemoteDataSource(get()) }
    single<EconomyRepository> { EconomyRepositoryImpl(get()) }
    single<OracleRepository> { OracleRepositoryImpl(get()) }
    single<TarotRepository> { TarotRepositoryImpl(get()) }
    single<TarotDeckCollectionRepository> { FirestoreTarotDeckCollectionRepository() }
    single<LastTarotReadingRepository> { SettingsLastTarotReadingRepository(get()) }
    single<SelectedTarotDeckRepository> { SettingsSelectedTarotDeckRepository(get()) }

    /**
     * Daily Ritual
     */
    single<FirebaseFirestore> { Firebase.firestore }
    single { SettingsDailyRitualRepository(get()) }
    single { SyncDailyRitualRepository(get(), get()) }
    single<DailyRitualRepository> { get<SyncDailyRitualRepository>() }
    single { SettingsHabitsRepository(get()) }
    single { SyncHabitsRepository(get(), get()) }
    single<HabitsRepository> { get<SyncHabitsRepository>() }
    single { LocalRitualCatalogRepository() }
    single { FirestoreBackedRitualCatalogRepository(local = get(), settingsFactory = get()).also { it.warmUp() } }
    single<RitualCatalogRepository> { get<FirestoreBackedRitualCatalogRepository>() }

    /**
     * Local user data cleanup (logout)
     */
    single<LocalUserDataRepository> { LocalUserDataRepositoryImpl(get(), get()) }
}
