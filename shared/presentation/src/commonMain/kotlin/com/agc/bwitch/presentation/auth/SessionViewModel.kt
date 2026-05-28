package com.agc.bwitch.presentation.auth

import com.agc.bwitch.domain.account.RestorePendingAccountDeletionUseCase
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
    private val authRepository: AuthRepository,
    private val restorePendingAccountDeletion: RestorePendingAccountDeletionUseCase? = null,
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
                    if (user == null) {
                        _uiState.update { SessionUiState(isLoading = false) }
                        return@collect
                    }

                    _uiState.update { it.copy(isLoading = true, error = null) }

                    runCatching { restorePendingAccountDeletion?.invoke(user.uid) }
                        .onSuccess {
                            _uiState.update { user.toLoggedInState() }
                        }
                        .onFailure { error ->
                            if (error.isAccountDeletionWindowExpired()) {
                                runCatching { authRepository.signOut() }
                                _uiState.update {
                                    SessionUiState(
                                        isLoading = false,
                                        isLoggedIn = false,
                                        error = error.message ?: "No se pudo restaurar la cuenta"
                                    )
                                }
                            } else {
                                println("BWITCH_ACCOUNT_DELETE restore_pending_non_blocking_error=${error.message}")
                                _uiState.update { user.toLoggedInState() }
                            }
                        }
                }
        }
    }

    private fun com.agc.bwitch.domain.auth.AuthUser.toLoggedInState(): SessionUiState = SessionUiState(
        isLoading = false,
        isLoggedIn = true,
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl,
        isAnonymous = isAnonymous,
        error = null,
    )

    private fun Throwable.isAccountDeletionWindowExpired(): Boolean {
        val haystack = buildString {
            append(message.orEmpty())
            append(' ')
            append(toString())
        }.lowercase()
        return "account_deletion_window_expired" in haystack ||
            "failed-precondition" in haystack ||
            "failed precondition" in haystack
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