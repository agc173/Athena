package com.agc.bwitch.data.di

import com.agc.bwitch.data.astrology.horoscope.HoroscopeRepositoryImpl
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import org.koin.core.module.Module
import org.koin.dsl.module

val dataKoinModule: Module = module {
    single<HoroscopeRepository> { HoroscopeRepositoryImpl() }
}
