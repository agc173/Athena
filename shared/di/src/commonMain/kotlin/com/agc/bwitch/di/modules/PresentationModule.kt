package com.agc.bwitch.di.modules

import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.presentation.astrology.synastry.SynastryViewModel
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.oracle.OracleAskViewModel
import com.agc.bwitch.presentation.oracle.OracleStatusViewModel
import com.agc.bwitch.presentation.pendulum.PendulumViewModel
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.presentation.moons.MoonStoreViewModel
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.presentation.rituals.DailyRitualViewModel
import com.agc.bwitch.presentation.rituals.HabitsViewModel
import com.agc.bwitch.presentation.tarot.TarotViewModel
import com.agc.bwitch.presentation.tarotcollection.TarotCollectionViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.presentation.userprofile.OnboardingProfileViewModel
import com.agc.bwitch.presentation.userprofile.SettingsViewModel
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel

val presentationModule: Module = module {

    /**
     * Navigator global (single source of truth de navegación)
     */
    single {
        Navigator(start = Destination.AuthGate)
    }

    /**
     * Session global
     */
    single {
        SessionViewModel(get())
    }
    single { AppLanguageViewModel(get(), get(), get()) }

    /**
     * Horoscope
     */
    factory { HoroscopeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }

    /**
     * BirthChart
     */
    factory { BirthChartViewModel(get(), get(), get(), get(), get(), get(), get()) }

    factory { SynastryViewModel(get(), get(), get(), get()) }

    factory { UserProfileViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { SettingsViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { OnboardingProfileViewModel(get(), get(), get(), get(), get(), get()) }

    factory { OracleStatusViewModel(get()) }
    factory { OracleAskViewModel(get(), get(), get(), get(), get()) }
    factory { TarotViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
    factory { TarotCollectionViewModel(get(), get(), get(), get()) }
    factory { MoonStoreViewModel(get(), get(), get(), get()) }
    single { EconomyViewModel(get(), get()) }
    factory { PendulumViewModel(get()) }
    factory { DailyRitualViewModel(get()) }
    factory { HabitsViewModel(get(), get(), get()) }

}
