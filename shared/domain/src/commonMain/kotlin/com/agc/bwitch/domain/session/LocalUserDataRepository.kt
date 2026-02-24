package com.agc.bwitch.domain.session

interface LocalUserDataRepository {
    suspend fun clear()
}