package com.agc.bwitch.domain.auth

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean
)
