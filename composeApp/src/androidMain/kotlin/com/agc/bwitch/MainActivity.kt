package com.agc.bwitch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import org.koin.compose.koinInject
import com.agc.bwitch.presentation.navigation.Navigator

class MainActivity : ComponentActivity() {
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
    }
}

