package com.agc.bwitch.di.modules

import com.agc.bwitch.data.di.dataKoinModule
import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    domainModule,
    dataKoinModule,
    presentationModule
)
