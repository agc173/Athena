package com.agc.bwitch.di.modules

import com.agc.bwitch.domain.astrology.birthchart.GenerateBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.GetBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.horoscope.DeriveZodiacSignUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetMonthlyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetWeeklyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveMonthlyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveWeeklyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.UnlockHoroscopeFutureDayUseCase
import com.agc.bwitch.domain.astrology.horoscope.IsHoroscopeDayUnlockedUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetHoroscopeFutureDayCostUseCase
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingGenerator
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.SetCurrentLanguageUseCase
import com.agc.bwitch.domain.moons.AddMoonsUseCase
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.GetMoonPacksUseCase
import com.agc.bwitch.domain.moons.HasEnoughMoonsUseCase
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.moons.SpendMoonsUseCase
import com.agc.bwitch.domain.notifications.RegisterPushTokenUseCase
import com.agc.bwitch.domain.notifications.UnregisterPushTokenUseCase
import com.agc.bwitch.domain.notifications.UpdatePushNotificationPreferencesUseCase
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.domain.settings.GetNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionCatalogUseCase
import com.agc.bwitch.domain.settings.GetSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.ObserveNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ObserveSubscriptionStatusUseCase
import com.agc.bwitch.domain.settings.RefreshPremiumEntitlementUseCase
import com.agc.bwitch.domain.settings.RestoreGooglePlayPurchasesUseCase
import com.agc.bwitch.domain.settings.RestorePurchasesUseCase
import com.agc.bwitch.domain.settings.UpdateNotificationSettingsUseCase
import com.agc.bwitch.domain.settings.ValidateGooglePlayPurchaseUseCase
import com.agc.bwitch.domain.tarot.GetSelectedTarotDeckUseCase
import com.agc.bwitch.domain.tarot.GetTarotDeckCollectionProgressUseCase
import com.agc.bwitch.domain.tarot.SetSelectedTarotDeckUseCase
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
    factory { GetWeeklyHoroscopeUseCase(get()) }
    factory { ObserveWeeklyHoroscopeUseCase(get()) }
    factory { GetMonthlyHoroscopeUseCase(get()) }
    factory { ObserveMonthlyHoroscopeUseCase(get()) }
    factory { PullDailyHoroscopeUseCase(get()) }
    factory { DeriveZodiacSignUseCase() }
    factory { GetHoroscopeFutureDayCostUseCase(get()) }
    factory { IsHoroscopeDayUnlockedUseCase(get()) }
    factory { UnlockHoroscopeFutureDayUseCase(get()) }

    // Birth Essence
    factory { GetBirthEssenceUseCase(get()) }
    factory { SaveBirthEssenceUseCase(get()) }
    factory { ObserveBirthEssenceUseCase(get()) }
    factory { PullBirthEssenceUseCase(get()) }
    factory { GenerateBirthEssenceUseCase(get()) }

    // Synastry
    factory { SynastryReadingGenerator() }

    // User profile
    factory { ObserveUserProfileUseCase(get()) }
    factory { GetUserProfileUseCase(get()) }
    factory { SaveUserProfileUseCase(get()) }
    factory { UploadAvatarUseCase(get()) }
    factory { ClearLocalUserDataUseCase(get()) }
    factory { PullUserProfileUseCase(get()) }

    // Settings
    factory { ObserveNotificationSettingsUseCase(get()) }
    factory { GetNotificationSettingsUseCase(get()) }
    factory { UpdateNotificationSettingsUseCase(get()) }
    factory { ObserveSubscriptionStatusUseCase(get()) }
    factory { GetSubscriptionStatusUseCase(get()) }
    factory { GetSubscriptionCatalogUseCase(get()) }
    factory { RestorePurchasesUseCase(get()) }
    factory { RestoreGooglePlayPurchasesUseCase(get()) }
    factory { RefreshPremiumEntitlementUseCase(get()) }
    factory { ValidateGooglePlayPurchaseUseCase(get()) }

    // Push notifications
    factory { RegisterPushTokenUseCase(get()) }
    factory { UnregisterPushTokenUseCase(get()) }
    factory { UpdatePushNotificationPreferencesUseCase(get()) }

    // Moons
    factory { GetMoonBalanceUseCase(get()) }
    factory { ObserveMoonBalanceUseCase(get()) }
    factory { AddMoonsUseCase(get()) }
    factory { SpendMoonsUseCase(get()) }
    factory { HasEnoughMoonsUseCase(get()) }
    factory { GetMoonPacksUseCase(get()) }
    factory { GetTarotDeckCollectionProgressUseCase(get()) }
    factory { GetSelectedTarotDeckUseCase(get()) }
    factory { SetSelectedTarotDeckUseCase(get()) }

    // Localization
    factory { ObserveCurrentLanguageUseCase(get()) }
    factory { ResolveCurrentLanguageUseCase(get()) }
    factory { SetCurrentLanguageUseCase(get()) }
}
