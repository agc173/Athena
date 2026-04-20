package com.agc.bwitch.domain.localization

import kotlinx.coroutines.flow.Flow

class ObserveCurrentLanguageUseCase(
    private val repository: AppLanguageRepository,
) {
    operator fun invoke(): Flow<AppLanguage> = repository.observeCurrentLanguage()
}
