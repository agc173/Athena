package com.agc.bwitch.domain.localization

class ResolveCurrentLanguageUseCase(
    private val repository: AppLanguageRepository,
) {
    suspend operator fun invoke(): AppLanguage = repository.resolveCurrentLanguage()
}
