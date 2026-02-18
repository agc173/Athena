package com.agc.bwitch.di.init

import com.agc.bwitch.di.modules.appModules
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(additionalModules: List<Module> = emptyList()): KoinApplication = startKoin {
    modules(appModules + additionalModules)
}

