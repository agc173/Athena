package com.agc.bwitch.di.modules

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {
    factory { GetDailyHoroscopeUseCase(get()) }
}
