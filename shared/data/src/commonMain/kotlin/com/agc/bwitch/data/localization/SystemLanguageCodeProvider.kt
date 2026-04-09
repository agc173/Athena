package com.agc.bwitch.data.localization

expect class SystemLanguageCodeProvider() {
    fun currentLanguageCode(): String?
}
