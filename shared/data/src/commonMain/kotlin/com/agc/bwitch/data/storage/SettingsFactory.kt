package com.agc.bwitch.data.storage

import com.russhwolf.settings.Settings

expect class SettingsFactory {
    fun create(name: String): Settings
}
