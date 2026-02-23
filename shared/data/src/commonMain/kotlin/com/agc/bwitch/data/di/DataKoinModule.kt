package com.agc.bwitch.data.di

import com.agc.bwitch.data.astrology.birthchart.SettingsBirthChartRepository
import com.agc.bwitch.data.astrology.birthchart.SyncBirthChartRepository
import com.agc.bwitch.data.astrology.horoscope.HoroscopeRepositoryImpl
import com.agc.bwitch.data.auth.FirebaseAuthRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.auth.AuthRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val dataKoinModule: Module = module {

    /**
     * Auth
     */
    single<AuthRepository> {
        FirebaseAuthRepository()
    }

    /**
     * Horoscope
     */
    single<HoroscopeRepository> {
        HoroscopeRepositoryImpl()
    }

    /**
     * BirthChart - LOCAL
     */
    single { SettingsBirthChartRepository(get()) }

    /**
     * BirthChart - SYNC (source of truth del dominio)
     */
    single<BirthChartRepository> {
        SyncBirthChartRepository(
            local = get(),
            authRepository = get()
        )
    }

}
