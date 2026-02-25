package com.agc.bwitch.di

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.presentation.auth.GoogleIdTokenProvider
import com.agc.bwitch.presentation.auth.GoogleIdTokenProviderAndroid
import com.russhwolf.settings.Settings
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(): Module = module {
    // Google Sign-In
    single<GoogleIdTokenProvider> { GoogleIdTokenProviderAndroid(androidContext()) }

    // Multiplatform Settings (Android impl)
    single { SettingsFactory(androidContext()) }
    single<Settings> { get<SettingsFactory>().create("bwitch") }
}