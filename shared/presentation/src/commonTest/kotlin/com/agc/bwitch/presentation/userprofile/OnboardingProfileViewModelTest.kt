package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.astrology.horoscope.DeriveZodiacSignUseCase
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.auth.AuthUser
import com.agc.bwitch.domain.userprofile.AvatarRepository
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import com.agc.bwitch.presentation.auth.SessionViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingProfileViewModelTest {

    @Test
    fun uploadAvatar_doesNotCallSaveProfileWhenUsernameMissing() = runTest {
        val userRepo = FakeUserProfileRepository()
        val avatarRepo = FakeAvatarRepository()

        val vm = OnboardingProfileViewModel(
            observe = ObserveUserProfileUseCase(userRepo),
            get = GetUserProfileUseCase(userRepo),
            save = SaveUserProfileUseCase(userRepo),
            uploadAvatar = UploadAvatarUseCase(avatarRepo),
            deriveZodiacSign = DeriveZodiacSignUseCase(),
            sessionVm = SessionViewModel(FakeAuthRepository())
        )

        vm.uploadAvatarAndSave("file:///tmp/avatar.png", "image/png")

        assertEquals(0, userRepo.saveCalls)
    }
}

private class FakeUserProfileRepository : UserProfileRepository {
    var saveCalls: Int = 0

    override fun observeUserProfile(): Flow<UserProfile?> = flowOf(null)

    override suspend fun getUserProfile(): UserProfile? = null

    override suspend fun saveUserProfile(profile: UserProfile) {
        saveCalls += 1
    }
}

private class FakeAvatarRepository : AvatarRepository {
    override suspend fun uploadAvatar(fileUri: String, mimeType: String?, previousUrl: String?): String =
        "https://cdn/avatar.png"
}

private class FakeAuthRepository : AuthRepository {
    override val authState = MutableStateFlow<AuthUser?>(null)

    override suspend fun signInWithEmail(email: String, password: String) = Unit
    override suspend fun signUpWithEmail(email: String, password: String) = Unit
    override suspend fun signOut() = Unit
    override suspend fun signInWithGoogleIdToken(idToken: String) = Unit
}
