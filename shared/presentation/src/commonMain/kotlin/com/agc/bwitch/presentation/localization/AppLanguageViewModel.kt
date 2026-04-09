package com.agc.bwitch.presentation.localization

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.SetCurrentLanguageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppLanguageUiState(
    val currentLanguage: AppLanguage = AppLanguage.fallback,
    val supportedLanguages: List<AppLanguage> = AppLanguage.supported,
)

class AppLanguageViewModel(
    private val resolveCurrentLanguage: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguage: ObserveCurrentLanguageUseCase,
    private val setCurrentLanguage: SetCurrentLanguageUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(AppLanguageUiState())
    val uiState: StateFlow<AppLanguageUiState> = _uiState

    init {
        scope.launch {
            val resolved = resolveCurrentLanguage()
            _uiState.update { it.copy(currentLanguage = resolved) }

            observeCurrentLanguage().collect { language ->
                _uiState.update { current -> current.copy(currentLanguage = language) }
            }
        }
    }

    fun onLanguageSelected(language: AppLanguage) {
        if (_uiState.value.currentLanguage == language) return

        scope.launch {
            setCurrentLanguage(language)
        }
    }
}
