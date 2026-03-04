package com.agc.bwitch

import androidx.compose.ui.window.ComposeUIViewController
import com.agc.bwitch.data.firebase.FirebaseBootstrapper

fun MainViewController() = ComposeUIViewController {
    FirebaseBootstrapper.init()
    App()
}
