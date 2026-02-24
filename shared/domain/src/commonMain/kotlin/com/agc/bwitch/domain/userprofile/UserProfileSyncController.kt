package com.agc.bwitch.domain.userprofile

interface UserProfileSyncController {
    suspend fun pull()
}