package com.agc.bwitch.data.userprofile

import com.agc.bwitch.data.storage.storageFileFromUri
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.userprofile.AvatarRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.storage.storage
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock

class FirebaseAvatarRepository(
    private val authRepository: AuthRepository
) : AvatarRepository {

    private val storage = Firebase.storage

    override suspend fun uploadAvatar(fileUri: String, mimeType: String?): String {
        val uid = currentUidOrNull() ?: error("No hay sesión")

        val ext = when (mimeType?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }

        val version = Clock.System.now().toEpochMilliseconds()
        val ref = storage.reference.child("users/$uid/profile/avatar_$version.$ext")

        val file = storageFileFromUri(fileUri)
        ref.putFile(file)

        return ref.getDownloadUrl()
    }

    private suspend fun currentUidOrNull(): String? =
        withTimeoutOrNull(2_000) {
            authRepository.authState
                .filterNotNull()
                .first()
                .uid
        }
}