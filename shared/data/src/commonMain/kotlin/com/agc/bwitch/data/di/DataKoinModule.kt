package com.agc.bwitch.data.di

import com.agc.bwitch.data.astrology.birthchart.SettingsBirthChartRepository
import com.agc.bwitch.data.astrology.birthchart.SyncBirthChartRepository
import com.agc.bwitch.data.astrology.horoscope.HoroscopeRepositoryImpl
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

val dataKoinModule: Module = module {

    /**
     * Auth
     */
    single<AuthRepository> { FirebaseAuthRepository() }

    /**
     * Horoscope
     */
    single<HoroscopeRepository> { HoroscopeRepositoryImpl() }

    /**
     * BirthChart - LOCAL
     */
    single { SettingsBirthChartRepository(get()) }

    /**
     * BirthChart - SYNC (source of truth del dominio)
     */
    single<BirthChartRepository> { SyncBirthChartRepository(get(), get()) }

    /**
     * UserProfile - LOCAL
     */
    single { SettingsUserProfileRepository(get()) }

    /**
     * UserProfile - SYNC (source of truth del dominio)
     */
    single<UserProfileRepository> { SyncUserProfileRepository(get(), get()) }

    /**
     * Avatar upload (Storage)
     */
    single<AvatarRepository> { FirebaseAvatarRepository(get()) }

    /**
     * Local user data cleanup (logout)
     */
    single<LocalUserDataRepository> { LocalUserDataRepositoryImpl(get(), get()) }
}