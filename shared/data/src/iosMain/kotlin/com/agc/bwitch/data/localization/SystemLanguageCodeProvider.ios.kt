package com.agc.bwitch.data.localization

import platform.Foundation.NSLocale

actual class SystemLanguageCodeProvider actual constructor() {
    actual fun currentLanguageCode(): String? =
        NSLocale.autoupdatingCurrentLocale.languageCode
            ?: NSLocale.currentLocale.languageCode
}
