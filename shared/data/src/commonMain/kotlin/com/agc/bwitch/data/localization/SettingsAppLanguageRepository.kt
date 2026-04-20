package com.agc.bwitch.data.localization

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

class SettingsAppLanguageRepository(
    settingsFactory: SettingsFactory,
    private val systemLanguageCodeProvider: SystemLanguageCodeProvider,
) : AppLanguageRepository {

    private val settings: Settings = settingsFactory.create("bwitch_language")
    private val languageKey = "current_language_code"

    private val currentLanguageState = MutableStateFlow(readSavedLanguageOrNull())

    override suspend fun resolveCurrentLanguage(): AppLanguage {
        val alreadyResolved = currentLanguageState.value
        if (alreadyResolved != null) return alreadyResolved

        val resolved = AppLanguage.fromCodeOrNull(systemLanguageCodeProvider.currentLanguageCode())
            ?: AppLanguage.fallback

        persistAndPublish(resolved)
        return resolved
    }

    override suspend fun getCurrentLanguage(): AppLanguage {
        return currentLanguageState.value ?: resolveCurrentLanguage()
    }

    override suspend fun setCurrentLanguage(language: AppLanguage) {
        persistAndPublish(language)
    }

    override fun observeCurrentLanguage(): Flow<AppLanguage> {
        return currentLanguageState
            .filterNotNull()
            .distinctUntilChanged()
    }

    private fun readSavedLanguageOrNull(): AppLanguage? {
        val rawCode = settings.getStringOrNull(languageKey)
        return AppLanguage.fromCodeOrNull(rawCode)
    }

    private fun persistAndPublish(language: AppLanguage) {
        settings.putString(languageKey, language.code)
        currentLanguageState.value = language
    }
}
