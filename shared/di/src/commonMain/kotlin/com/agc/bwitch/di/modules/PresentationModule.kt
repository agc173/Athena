package com.agc.bwitch.di.modules

import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val presentationModule: Module = module {
    factory {
        val dispatcher = runCatching { get<CoroutineDispatcher>() }.getOrElse { Dispatchers.Default }
        HoroscopeViewModel(
            getDailyHoroscopeUseCase = get(),
            dispatcher = dispatcher,
        )
    }
}
