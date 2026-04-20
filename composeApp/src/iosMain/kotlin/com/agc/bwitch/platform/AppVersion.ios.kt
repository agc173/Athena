package com.agc.bwitch.platform

import platform.Foundation.NSBundle

actual fun getAppVersionLabel(): String {
    val bundle = NSBundle.mainBundle
    val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String
    val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String

    return when {
        !version.isNullOrBlank() && !build.isNullOrBlank() -> "$version ($build)"
        !version.isNullOrBlank() -> version
        !build.isNullOrBlank() -> build
        else -> "-"
    }
}
