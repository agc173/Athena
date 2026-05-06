package com.agc.bwitch.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authState: Flow<AuthUser?> // null = logged out

    suspend fun signInWithEmail(email: String, password: String)
    suspend fun signUpWithEmail(email: String, password: String)
    suspend fun sendPasswordResetEmail(email: String) {
        error("Password reset is not supported by this auth repository.")
    }
    suspend fun signOut()
    suspend fun signInWithGoogleIdToken(idToken: String)
}
