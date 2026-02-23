package com.agc.bwitch.domain.userprofile

interface AvatarRepository {
    suspend fun uploadAvatar(
        fileUri: String,
        mimeType: String? = null
    ): String
}