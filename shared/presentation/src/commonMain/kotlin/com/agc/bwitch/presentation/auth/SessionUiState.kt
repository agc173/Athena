package com.agc.bwitch.presentation.auth

data class SessionUiState(
    val isLoggedIn: Boolean = false,
    val uid: String? = null,
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val error: String? = null
)
