package com.agc.bwitch

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import com.agc.bwitch.ads.AndroidRewardedAdsService
import com.agc.bwitch.data.moons.billing.googleplay.GooglePlayMoonPackBillingDataSource
import org.koin.compose.koinInject
import org.koin.java.KoinJavaComponent.inject
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.notifications.PushIntentRouter

class MainActivity : ComponentActivity() {
    private val rewardedAdsService: AndroidRewardedAdsService by inject(AndroidRewardedAdsService::class.java)
    private val moonPackBillingDataSource: GooglePlayMoonPackBillingDataSource by inject(GooglePlayMoonPackBillingDataSource::class.java)
    private val navigator: Navigator by inject(Navigator::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Opcional: solo para verificar / debug
        val clientId = getString(R.string.default_web_client_id)
        // println("default_web_client_id=$clientId")

        setContent {
            val navigator: Navigator = koinInject()

            BackHandler(enabled = navigator.canGoBack()) {
                navigator.goBack()
            }

            App()
        }

        PushIntentRouter.handle(intent, navigator)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        PushIntentRouter.handle(intent, navigator)
    }

    override fun onStart() {
        super.onStart()
        rewardedAdsService.bindActivity(this)
        moonPackBillingDataSource.bindActivity(this)
    }

    override fun onStop() {
        rewardedAdsService.bindActivity(null)
        moonPackBillingDataSource.bindActivity(null)
        super.onStop()
    }
}
