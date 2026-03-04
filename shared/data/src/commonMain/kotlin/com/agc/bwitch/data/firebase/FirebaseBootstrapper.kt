package com.agc.bwitch.data.firebase

object FirebaseBootstrapper {
    @Volatile
    private var initialized = false

    fun init() {
        if (initialized) return

        synchronized(this) {
            if (initialized) return
            installAppCheckDebugProvider()
            initialized = true
        }
    }
}

expect fun installAppCheckDebugProvider()
