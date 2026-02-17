package com.agc.bwitch.di

import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    // Dispatcher IO para Android
    single<CoroutineDispatcher> { Dispatchers.IO }

    // Engine de Ktor para Android
    single { OkHttp.create() }
}
