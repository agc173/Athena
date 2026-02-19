package com.agc.bwitch.presentation.auth

import com.agc.bwitch.domain.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState

    init {
        scope.launch {
            authRepository.authState
                .catch { e -> _uiState.update { it.copy(error = e.message) } }
                .collect { user ->
                    _uiState.update {
                        SessionUiState(
                            isLoggedIn = user != null,
                            uid = user?.uid,
                            email = user?.email,
                            isAnonymous = user?.isAnonymous ?: false
                        )
                    }
                }
        }
    }

    fun signInAnonymously() = scope.launch {
        runCatching { authRepository.signInAnonymously() }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun signOut() = scope.launch {
        runCatching { authRepository.signOut() }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }
}
