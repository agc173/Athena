package com.agc.bwitch.domain.localization

import kotlinx.coroutines.flow.Flow

interface AppLanguageRepository {
    suspend fun resolveCurrentLanguage(): AppLanguage
    suspend fun getCurrentLanguage(): AppLanguage
    suspend fun setCurrentLanguage(language: AppLanguage)
    fun observeCurrentLanguage(): Flow<AppLanguage>
}
