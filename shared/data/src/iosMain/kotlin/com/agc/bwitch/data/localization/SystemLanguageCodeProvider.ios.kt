package com.agc.bwitch.data.localization

import platform.Foundation.NSUserDefaults

actual class SystemLanguageCodeProvider actual constructor() {
    actual fun currentLanguageCode(): String? {
        val value = NSUserDefaults.standardUserDefaults.objectForKey("AppleLanguages")
        val list = value as? List<*>
        return list?.firstOrNull() as? String
    }
}
