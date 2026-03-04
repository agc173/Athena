package com.agc.bwitch

import android.app.Application
import com.agc.bwitch.data.firebase.FirebaseBootstrapper
import com.agc.bwitch.di.init.initKoin
import com.agc.bwitch.di.platformModule

class BWitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseBootstrapper.init()
        initKoin(additionalModules = listOf(platformModule(this)))
    }
}