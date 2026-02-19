package com.agc.bwitch.di.modules

import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.presentation.navigation.Navigator
import org.koin.core.module.Module
import org.koin.dsl.module

val presentationModule: Module = module {
    single { Navigator() }
    factory { HoroscopeViewModel(get()) }
}


