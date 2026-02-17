package com.agc.bwitch.di.modules

import org.koin.core.module.Module

val appModules: List<Module> = listOf(
    domainModule,
    dataModule,
    presentationModule
)
