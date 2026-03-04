package com.agc.bwitch.data.firebase

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.appcheck.appCheck
import dev.gitlive.firebase.appcheck.debugAppCheckProviderFactory
import kotlin.native.Platform

actual fun installAppCheckDebugProvider() {
    if (!Platform.isDebugBinary) {
        // TODO: En PROD usar App Attest/DeviceCheck provider con Firebase.appCheck.
        return
    }

    Firebase.appCheck.installAppCheckProviderFactory(
        debugAppCheckProviderFactory
    )
}
