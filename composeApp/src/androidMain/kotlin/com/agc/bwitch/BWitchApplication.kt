package com.agc.bwitch

import android.app.Application
import com.agc.bwitch.di.init.initKoin
import com.agc.bwitch.di.platformModule

class BWitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(additionalModules = listOf(platformModule))
    }
}
