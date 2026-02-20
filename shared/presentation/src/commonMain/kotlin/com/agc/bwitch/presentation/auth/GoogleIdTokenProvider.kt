package com.agc.bwitch.presentation.auth

interface GoogleIdTokenProvider {
    suspend fun getIdToken(): String
}