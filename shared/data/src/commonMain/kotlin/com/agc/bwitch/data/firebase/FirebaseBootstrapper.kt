package com.agc.bwitch.data.firebase

object FirebaseBootstrapper {
    private var appCheckInstalled = false

    fun init() {
        if (appCheckInstalled) return
        installAppCheckDebugProvider()
        appCheckInstalled = true
    }
}

expect fun installAppCheckDebugProvider()
