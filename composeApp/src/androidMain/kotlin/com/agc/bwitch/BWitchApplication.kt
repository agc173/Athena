package com.agc.bwitch

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
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
        logAdMobAppId()

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

    private fun logAdMobAppId() {
        val appId = runCatching {
            packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
                ?.getString("com.google.android.gms.ads.APPLICATION_ID")
        }.getOrNull() ?: "missing"

        Log.i("BWitchApplication", "BWITCH_ADMOB_APP_ID=$appId")
    }
}
