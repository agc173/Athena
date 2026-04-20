package com.agc.bwitch.data.localization

import java.util.Locale

actual class SystemLanguageCodeProvider actual constructor() {
    actual fun currentLanguageCode(): String? = Locale.getDefault().toLanguageTag()
}
