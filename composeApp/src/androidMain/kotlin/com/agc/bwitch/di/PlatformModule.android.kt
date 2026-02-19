package com.agc.bwitch.di

import android.app.Application
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module
import com.agc.bwitch.data.storage.SettingsFactory


fun platformModule(app: Application): Module = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    single { OkHttp.create() }
    single { SettingsFactory(app) }


    single<com.agc.bwitch.data.storage.SettingsFactory> {
        com.agc.bwitch.data.storage.SettingsFactory(app)
    }
}
