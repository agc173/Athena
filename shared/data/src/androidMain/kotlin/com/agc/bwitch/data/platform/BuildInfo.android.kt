package com.agc.bwitch.data.platform

import com.agc.bwitch.shared.data.BuildConfig

actual object BuildInfo {
    actual val isDebug: Boolean = BuildConfig.DEBUG
}
