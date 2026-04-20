package com.agc.bwitch.platform

import com.agc.bwitch.BuildConfig

actual fun getAppVersionLabel(): String {
    val versionName = BuildConfig.VERSION_NAME.takeUnless { it.isBlank() }
    val versionCode = BuildConfig.VERSION_CODE.toString().takeUnless { it.isBlank() }

    return when {
        !versionName.isNullOrBlank() && !versionCode.isNullOrBlank() -> "$versionName ($versionCode)"
        !versionName.isNullOrBlank() -> versionName
        !versionCode.isNullOrBlank() -> versionCode
        else -> "-"
    }
}
