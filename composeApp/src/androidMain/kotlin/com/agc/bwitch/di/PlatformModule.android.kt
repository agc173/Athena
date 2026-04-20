package com.agc.bwitch.di

import android.app.Application
import android.content.Context
import com.agc.bwitch.audio.AndroidTarotHaptics
import com.agc.bwitch.audio.AndroidTarotSoundPlayer
import com.agc.bwitch.audio.TarotHaptics
import com.agc.bwitch.audio.TarotSoundPlayer
import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.data.settings.billing.googleplay.GooglePlaySubscriptionBillingDataSource
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.presentation.auth.GoogleIdTokenProvider
import com.agc.bwitch.presentation.auth.GoogleIdTokenProviderAndroid
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
    single<SubscriptionBillingDataSource>(override = true) { GooglePlaySubscriptionBillingDataSource(app) }
    single<TarotSoundPlayer> { AndroidTarotSoundPlayer(app) }
    single<TarotHaptics> { AndroidTarotHaptics(app) }

    // Needs Activity context (passed from Compose via parametersOf(context))
    factory<GoogleIdTokenProvider> { (ctx: Context) ->
        GoogleIdTokenProviderAndroid(ctx)
    }
}
