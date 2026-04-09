package com.agc.bwitch.domain.localization

class SetCurrentLanguageUseCase(
    private val repository: AppLanguageRepository,
) {
    suspend operator fun invoke(language: AppLanguage) {
        repository.setCurrentLanguage(language)
    }
}
