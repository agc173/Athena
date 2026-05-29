package com.agc.bwitch.di

import android.app.Application
import android.content.Context
import com.agc.bwitch.analytics.AndroidFirebaseAnalyticsTracker
import com.agc.bwitch.ads.AndroidRewardedAdsService
import com.agc.bwitch.audio.AndroidTarotHaptics
import com.agc.bwitch.audio.AndroidTarotSoundPlayer
import com.agc.bwitch.audio.TarotHaptics
import com.agc.bwitch.audio.TarotSoundPlayer
import com.agc.bwitch.data.moons.MoonPackBillingDataSource
import com.agc.bwitch.data.moons.billing.googleplay.GooglePlayMoonPackBillingDataSource
import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.data.settings.billing.googleplay.GooglePlaySubscriptionBillingDataSource
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.notifications.AndroidPushNotificationManager
import com.agc.bwitch.notifications.AndroidPushTokenSynchronizer
import com.agc.bwitch.presentation.auth.GoogleIdTokenProvider
import com.agc.bwitch.presentation.auth.GoogleIdTokenProviderAndroid
import com.agc.bwitch.presentation.ads.RewardedAdsService
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import com.russhwolf.settings.Settings

fun platformModule(app: Application): Module = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single { OkHttp.create() }
    single { SettingsFactory(app) }
    single<Settings> { get<SettingsFactory>().create("bwitch") }
    single { GooglePlaySubscriptionBillingDataSource(app) }
    single<SubscriptionBillingDataSource> { get<GooglePlaySubscriptionBillingDataSource>() }
    single { GooglePlayMoonPackBillingDataSource(app) }
    single<MoonPackBillingDataSource> { get<GooglePlayMoonPackBillingDataSource>() }
    single<AnalyticsTracker> { AndroidFirebaseAnalyticsTracker(app) }
    single { AndroidRewardedAdsService() }
    single<RewardedAdsService> { get<AndroidRewardedAdsService>() }
    single<TarotSoundPlayer> { AndroidTarotSoundPlayer(app) }
    single { AndroidPushNotificationManager(app) }
    single { AndroidPushTokenSynchronizer(app, get(), get(), get()) }
    single<TarotHaptics> { AndroidTarotHaptics(app) }

    // Needs Activity context (passed from Compose via parametersOf(context))
    factory<GoogleIdTokenProvider> { (ctx: Context) ->
        GoogleIdTokenProviderAndroid(ctx)
    }
}
