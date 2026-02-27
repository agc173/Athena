package com.agc.bwitch.data.di

import com.agc.bwitch.data.astrology.birthchart.SettingsBirthChartRepository
import com.agc.bwitch.data.astrology.birthchart.SyncBirthChartRepository
import com.agc.bwitch.data.auth.FirebaseAuthRepository
import com.agc.bwitch.data.session.LocalUserDataRepositoryImpl
import com.agc.bwitch.data.userprofile.FirebaseAvatarRepository
import com.agc.bwitch.data.userprofile.SettingsUserProfileRepository
import com.agc.bwitch.data.userprofile.SyncUserProfileRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.session.LocalUserDataRepository
import com.agc.bwitch.domain.userprofile.AvatarRepository
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.domain.astrology.birthchart.BirthChartSyncController
import com.agc.bwitch.domain.userprofile.UserProfileSyncController
import com.agc.bwitch.data.astrology.horoscope.SettingsHoroscopeDailyRepository
import com.agc.bwitch.data.astrology.horoscope.SyncHoroscopeDailyRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.data.astrology.horoscope.SettingsHoroscopePullMarkerRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker

val dataKoinModule: Module = module {

    /**
     * Auth
     */
    single<AuthRepository> { FirebaseAuthRepository() }

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

    /**
     * BirthChart - LOCAL
     */
    single { SettingsBirthChartRepository(get()) }

    /**
     * BirthChart - SYNC (source of truth del dominio)
     */
    single { SyncBirthChartRepository(get(), get()) }
    single<BirthChartRepository> { get<SyncBirthChartRepository>() }
    single<BirthChartSyncController> { get<SyncBirthChartRepository>() }

    /**
     * UserProfile - LOCAL
     */
    single { SettingsUserProfileRepository(get()) }

    /**
     * UserProfile - SYNC (source of truth del dominio)
     */
    single { SyncUserProfileRepository(get(), get()) }
    single<UserProfileRepository> { get<SyncUserProfileRepository>() }
    single<UserProfileSyncController> { get<SyncUserProfileRepository>() }

    /**
     * Avatar upload (Storage)
     */
    single<AvatarRepository> { FirebaseAvatarRepository(get()) }

    /**
     * Local user data cleanup (logout)
     */
    single<LocalUserDataRepository> { LocalUserDataRepositoryImpl(get(), get()) }
}