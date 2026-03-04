package com.agc.bwitch.data.firebase

import com.agc.bwitch.shared.data.BuildConfig
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.appcheck.appCheck
import dev.gitlive.firebase.appcheck.debugAppCheckProviderFactory

actual fun installAppCheckDebugProvider() {
    if (!BuildConfig.DEBUG) {
        // TODO: En PROD usar Play Integrity provider con Firebase.appCheck.
        return
    }

    Firebase.appCheck.installAppCheckProviderFactory(
        debugAppCheckProviderFactory
    )
}
