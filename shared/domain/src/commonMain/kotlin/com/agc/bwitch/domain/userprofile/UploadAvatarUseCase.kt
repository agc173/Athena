package com.agc.bwitch.domain.userprofile

class UploadAvatarUseCase(
    private val avatarRepository: AvatarRepository
) {
    suspend operator fun invoke(
        fileUri: String,
        mimeType: String? = null,
        previousUrl: String? = null
    ): String = avatarRepository.uploadAvatar(fileUri, mimeType, previousUrl)
}