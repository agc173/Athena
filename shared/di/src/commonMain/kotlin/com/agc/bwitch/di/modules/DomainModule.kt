package com.agc.bwitch.di.modules

import com.agc.bwitch.domain.astrology.birthchart.GetBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthDataUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.domain.userprofile.PullUserProfileUseCase

val domainModule: Module = module {

    // Horoscope
    factory { GetDailyHoroscopeUseCase(get()) }

    // BirthChart
    factory { GetBirthDataUseCase(get()) }
    factory { SaveBirthDataUseCase(get()) }
    factory { ObserveBirthDataUseCase(get()) }

    factory { ObserveUserProfileUseCase(get()) }
    factory { GetUserProfileUseCase(get()) }
    factory { SaveUserProfileUseCase(get()) }
    factory { com.agc.bwitch.domain.userprofile.UploadAvatarUseCase(get()) }
    factory { UploadAvatarUseCase(get()) }
    factory { ClearLocalUserDataUseCase(get()) }
    factory { PullUserProfileUseCase(get()) }
}
