package com.agc.bwitch.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?> // null = logged out

    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun signOut()
    suspend fun signInWithGoogleIdToken(idToken: String)
}
