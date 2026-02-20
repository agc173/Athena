package com.agc.bwitch.di.modules

import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.presentation.navigation.Navigator
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.presentation.auth.SessionViewModel

val presentationModule: Module = module {
    single { Navigator() }
    factory { HoroscopeViewModel(get()) }
    factory { BirthChartViewModel(get(), get(), get()) }
    single { SessionViewModel(get()) }
}


