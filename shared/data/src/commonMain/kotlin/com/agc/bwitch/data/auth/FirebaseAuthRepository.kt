package com.agc.bwitch.data.auth

import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.auth.AuthUser
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.FirebaseAuth
import dev.gitlive.firebase.auth.FirebaseUser
import dev.gitlive.firebase.auth.GoogleAuthProvider
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuthRepository : AuthRepository {

    private val auth: FirebaseAuth = Firebase.auth

    override val authState: Flow<AuthUser?> =
        auth.authStateChanged.map { user -> user.toDomain() }

    override suspend fun signInWithGoogleIdToken(idToken: String) {
        val credential = GoogleAuthProvider.credential(idToken, null)
        auth.signInWithCredential(credential)
    }


    override suspend fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
    }

    override suspend fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email, null)
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}


private fun FirebaseUser?.toDomain(): AuthUser? = this?.let { user ->
    AuthUser(
        uid = user.uid,
        email = user.email,
        isAnonymous = user.isAnonymous,
        displayName = user.displayName,
        photoUrl = user.photoURL
    )
}
