package com.agc.bwitch.domain.auth

data class AuthUser(
    val uid: String,
    val email: String?,
    val isAnonymous: Boolean,
    val displayName: String? = null,
    val photoUrl: String? = null
)
