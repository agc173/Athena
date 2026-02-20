package com.agc.bwitch.di

import com.agc.bwitch.presentation.auth.GoogleIdTokenProvider
import com.agc.bwitch.presentation.auth.GoogleIdTokenProviderAndroid
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(): Module = module {
    single<GoogleIdTokenProvider> { GoogleIdTokenProviderAndroid(androidContext()) }
}