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
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.eclipticGeoMoon
import io.github.cosinekitty.astronomy.illumination
import io.github.cosinekitty.astronomy.moonPhase
import io.github.cosinekitty.astronomy.siderealTime
import io.github.cosinekitty.astronomy.sunPosition
import org.koin.core.context.GlobalContext

class BWitchApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        logAdMobAppId()
        runNatalChartSpikeSmokeTest()

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

    private fun runNatalChartSpikeSmokeTest() {
        // TODO(feature/basic-natal-chart): remove smoke test after spike validation
        if (!BuildConfig.DEBUG) return

        // TODO(feature/basic-natal-chart): remove smoke test after spike validation
        val birthTimeUtc = Time(1990, 7, 15, 12, 0, 0.0)
        val sun = sunPosition(birthTimeUtc)
        val moon = eclipticGeoMoon(birthTimeUtc)
        val moonIllumination = illumination(Body.Moon, birthTimeUtc)
        val moonPhase = moonPhase(birthTimeUtc)
        val siderealTime = siderealTime(birthTimeUtc)

        // TODO(feature/basic-natal-chart): remove smoke test after spike validation
        // Interesting reusable APIs for the final implementation: sunPosition().elon,
        // eclipticGeoMoon().lon/lat, moonPhase(), siderealTime(), and the rotation* APIs
        // for deriving local horizon/ecliptic data needed by an ascendant calculation.
        Log.i(
            "NatalChartSpike",
            """
            NatalChartSpike:
            - Time UTC: $birthTimeUtc
            - Sun ecliptic longitude: ${sun.elon} deg; latitude: ${sun.elat} deg; vector: ${sun.vec}
            - Moon ecliptic longitude: ${moon.lon} deg; latitude: ${moon.lat} deg; distance: ${moon.dist} AU
            - Moon phase angle: $moonPhase deg
            - Moon illuminated fraction: ${moonIllumination.phaseFraction}
            - Greenwich apparent sidereal time: $siderealTime hours
            """.trimIndent()
        )
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
