package com.agc.bwitch.presentation.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.agc.bwitch.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

class GoogleIdTokenProviderAndroid(
    private val context: Context
) : GoogleIdTokenProvider {

    override suspend fun getIdToken(): String {
        val credentialManager = CredentialManager.create(context)

        val serverClientId = context.getString(R.string.default_web_client_id)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(serverClientId)
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val result = credentialManager.getCredential(context, request)
        val credential = result.credential

        val google = GoogleIdTokenCredential.createFrom(credential.data)
        return google.idToken
    }
}