package com.agc.bwitch.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.Module
import org.koin.dsl.module

val platformModule: Module = module {
    // Dispatcher recomendado en iOS (más seguro que IO)
    single<CoroutineDispatcher> { Dispatchers.Default }

    // Engine Ktor iOS
    single<HttpClientEngine> { Darwin.create() }
}
