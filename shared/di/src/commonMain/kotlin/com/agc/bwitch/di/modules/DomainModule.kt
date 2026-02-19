package com.agc.bwitch.di.modules

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.domain.astrology.birthchart.GetBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthDataUseCase



val domainModule: Module = module {
    factory { GetDailyHoroscopeUseCase(get()) }
    factory { GetBirthDataUseCase(get()) }
    factory { SaveBirthDataUseCase(get()) }
    factory { ObserveBirthDataUseCase(get()) }


}
