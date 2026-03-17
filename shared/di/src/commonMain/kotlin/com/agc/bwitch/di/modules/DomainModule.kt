package com.agc.bwitch.di.modules

import com.agc.bwitch.domain.astrology.birthchart.GetBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthChartUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthDataUseCase
import com.agc.bwitch.domain.astrology.horoscope.DeriveZodiacSignUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.PullUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

val domainModule: Module = module {

    // Horoscope
    factory { GetDailyHoroscopeUseCase(get()) }
    factory { ObserveDailyHoroscopeUseCase(get()) }
    factory { PullDailyHoroscopeUseCase(get()) }
    factory { DeriveZodiacSignUseCase() }

    // BirthChart
    factory { GetBirthDataUseCase(get()) }
    factory { SaveBirthDataUseCase(get()) }
    factory { ObserveBirthDataUseCase(get()) }
    factory { PullBirthChartUseCase(get()) }

    // User profile
    factory { ObserveUserProfileUseCase(get()) }
    factory { GetUserProfileUseCase(get()) }
    factory { SaveUserProfileUseCase(get()) }
    factory { UploadAvatarUseCase(get()) }
    factory { ClearLocalUserDataUseCase(get()) }
    factory { PullUserProfileUseCase(get()) }
}
