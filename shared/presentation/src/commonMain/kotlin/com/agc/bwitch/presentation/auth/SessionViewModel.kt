package com.agc.bwitch.presentation.auth

import com.agc.bwitch.domain.auth.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SessionViewModel(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            authRepository.authState
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                }
                .collect { user ->
                    _uiState.update {
                        SessionUiState(
                            isLoading = false,
                            isLoggedIn = user != null,
                            uid = user?.uid,
                            email = user?.email,
                            displayName = user?.displayName,
                            photoUrl = user?.photoUrl,
                            isAnonymous = user?.isAnonymous ?: false,
                            error = null
                        )
                    }
                }
        }
    }

    fun signInWithEmail(email: String, password: String) = scope.launch {
        runCatching { authRepository.signInWithEmail(email, password) }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun signUpWithEmail(email: String, password: String) = scope.launch {
        runCatching { authRepository.signUpWithEmail(email, password) }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun sendPasswordResetEmail(email: String, onResult: (Boolean) -> Unit = {}) = scope.launch {
        runCatching { authRepository.sendPasswordResetEmail(email) }
            .onSuccess { onResult(true) }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                onResult(false)
            }
    }

    fun signInWithGoogle(idToken: String) = scope.launch {
        runCatching { authRepository.signInWithGoogleIdToken(idToken) }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun signOut() = scope.launch {
        runCatching { authRepository.signOut() }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
    }

    fun clear() {
        scope.cancel()
    }
}