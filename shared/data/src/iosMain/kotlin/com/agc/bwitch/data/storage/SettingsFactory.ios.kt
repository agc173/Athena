package com.agc.bwitch.data.storage

import com.russhwolf.settings.Settings
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

actual class SettingsFactory {
    actual fun create(name: String): Settings =
        NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}
