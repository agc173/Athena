package com.agc.bwitch.presentation.auth

data class SessionUiState(
    val isLoading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val uid: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val isAnonymous: Boolean = false,
    val error: String? = null
)
