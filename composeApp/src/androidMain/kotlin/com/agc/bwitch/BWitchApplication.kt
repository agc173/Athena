package com.agc.bwitch

import android.app.Application
import android.content.pm.PackageManager
import android.util.Log
import com.agc.bwitch.BuildConfig
import com.agc.bwitch.di.init.initKoin
import com.agc.bwitch.di.platformModule
import com.agc.bwitch.notifications.AndroidNotificationChannels
import com.agc.bwitch.notifications.AndroidPushTokenSynchronizer
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import org.koin.core.context.GlobalContext

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
            // Requires Firebase Console App Check registration + Play Integrity API setup for the release app.
            FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                PlayIntegrityAppCheckProviderFactory.getInstance()
            )
        }

        MobileAds.initialize(this)
        AndroidNotificationChannels.create(this)

        initKoin(additionalModules = listOf(platformModule(this)))
        GlobalContext.get().get<AndroidPushTokenSynchronizer>().start()
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
