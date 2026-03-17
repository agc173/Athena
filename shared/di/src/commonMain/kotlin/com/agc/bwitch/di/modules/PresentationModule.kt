package com.agc.bwitch.di.modules

import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.oracle.OracleAskViewModel
import com.agc.bwitch.presentation.oracle.OracleStatusViewModel
import com.agc.bwitch.presentation.pendulum.PendulumViewModel
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.presentation.tarot.TarotViewModel
import org.koin.core.module.Module
import org.koin.dsl.module
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

    /**
     * Horoscope
     */
    factory { HoroscopeViewModel(get(), get(), get(), get(), get()) }

    /**
     * BirthChart
     */
    factory { BirthChartViewModel(get(), get(), get(), get()) }


    factory { UserProfileViewModel(get(), get(), get(), get(), get(), get(), get()) }

    factory { OracleStatusViewModel(get()) }
    factory { OracleAskViewModel(get()) }
    factory { TarotViewModel(get()) }
    factory { PendulumViewModel() }

}


