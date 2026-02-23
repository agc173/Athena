package com.agc.bwitch.di.modules

import com.agc.bwitch.domain.astrology.birthchart.GetBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthDataUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {

    // Horoscope
    factory { GetDailyHoroscopeUseCase(get()) }

    // BirthChart
    factory { GetBirthDataUseCase(get()) }
    factory { SaveBirthDataUseCase(get()) }
    factory { ObserveBirthDataUseCase(get()) }

    // UserProfile (lo añadimos cuando creemos el módulo)
    // factory { GetUserProfileUseCase(get()) }
    // factory { SaveUserProfileUseCase(get()) }
    // factory { ObserveUserProfileUseCase(get()) }
}
