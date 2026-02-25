package com.agc.bwitch.di

import android.app.Application
import android.content.Context
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

    // Needs Activity context (passed from Compose via parametersOf(context))
    factory<GoogleIdTokenProvider> { (ctx: Context) ->
        GoogleIdTokenProviderAndroid(ctx)
    }
}
