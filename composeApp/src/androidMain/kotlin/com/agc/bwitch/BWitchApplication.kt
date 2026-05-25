package com.agc.bwitch

import android.app.Application
import com.agc.bwitch.BuildConfig
import com.agc.bwitch.di.init.initKoin
import com.agc.bwitch.di.platformModule
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class BWitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        if (BuildConfig.DEBUG) {
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )
        } else {
            // TODO: Integrar proveedor de App Check con Play Integrity para release.
        }

        MobileAds.initialize(this)

        initKoin(additionalModules = listOf(platformModule(this)))
    }
}
