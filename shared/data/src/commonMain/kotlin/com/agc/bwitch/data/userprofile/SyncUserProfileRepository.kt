package com.agc.bwitch.data.userprofile

import com.agc.bwitch.data.sync.LocalStore
import com.agc.bwitch.data.sync.RemoteStore
import com.agc.bwitch.data.sync.SyncEngine
import com.agc.bwitch.data.sync.Timestamped
import com.agc.bwitch.data.userprofile.dto.SaveUserProfileRequestDto
import com.agc.bwitch.data.userprofile.dto.SaveUserProfileResponseDto
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.userprofile.UsernameRules
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import com.agc.bwitch.domain.userprofile.UserProfileSyncController
import com.agc.bwitch.data.functions.FunctionsClient
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

class SyncUserProfileRepository(
    private val localRepo: SettingsUserProfileRepository,
    private val authRepository: AuthRepository,
    private val functionsClient: FunctionsClient,
) : UserProfileRepository, UserProfileSyncController {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localStore = object : LocalStore<UserProfile> {
        override fun observe() = localRepo.observeUserProfile()
        override suspend fun get() = localRepo.getUserProfile()
        override suspend fun save(value: UserProfile, updatedAtEpochMillis: Long) {
            localRepo.saveUserProfileWithUpdatedAt(value, updatedAtEpochMillis)
        }
        override fun localUpdatedAtEpochMillisOrNull(): Long? = localRepo.getLocalUpdatedAtEpochMillisOrNull()
    }

    private val remoteStore = object : RemoteStore<UserProfile> {
        override suspend fun fetch(): Timestamped<UserProfile> {
            val uid = currentUidOrNull() ?: return Timestamped(null, null)
            val dto = fetchRemote(uid) ?: return Timestamped(null, null)
            return Timestamped(dto.toUserProfile(), dto.updatedAtEpochMillis)
        }

        override suspend fun push(value: UserProfile, updatedAtEpochMillis: Long) {
            pushThroughCallable(value, updatedAtEpochMillis)
        }
    }

    private val engine = SyncEngine(
        scope = scope,
        local = localStore,
        remote = remoteStore
    )

    override fun observeUserProfile() = engine.observe()

    override suspend fun getUserProfile(): UserProfile? = engine.get()

    override suspend fun saveUserProfile(profile: UserProfile) {
        val now = Clock.System.now().toEpochMilliseconds()
        val normalizedProfile = profile.copy(username = UsernameRules.normalize(profile.username))
        pushThroughCallable(normalizedProfile, now)
        localRepo.saveUserProfileWithUpdatedAt(normalizedProfile, now)
    }

    override suspend fun pull() {
        engine.pull()
    }

    private suspend fun currentUidOrNull(): String? =
        withTimeoutOrNull(2_000) {
            authRepository.authState
                .filterNotNull()
                .first()
                .uid
        }

    private fun doc(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("profile")
            .document("current")

    private suspend fun fetchRemote(uid: String): UserProfileRemoteDto? {
        val snap = doc(uid).get()
        if (!snap.exists) return null
        return snap.data(UserProfileRemoteDto.serializer())
    }


    private suspend fun pushThroughCallable(value: UserProfile, updatedAtEpochMillis: Long) {
        val normalizedUsername = UsernameRules.normalize(value.username)
        if (normalizedUsername != null && !UsernameRules.isValid(normalizedUsername)) {
            throw IllegalArgumentException("Username inválido. Usa 3-30 caracteres: letras, números, punto o guion bajo")
        }

        val request = SaveUserProfileRequestDto(
            displayName = value.displayName,
            photoUrl = value.photoUrl,
            email = value.email,
            username = normalizedUsername,
            birthDate = value.birthDate,
            zodiacSign = value.zodiacSign,
            birthEssenceSummary = value.birthEssenceSummary,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )

        when (
            val result = functionsClient.call(
                name = SAVE_USER_PROFILE_CALLABLE,
                data = request,
                requestSerializer = SaveUserProfileRequestDto.serializer(),
                responseSerializer = SaveUserProfileResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> {
                // El callable persiste perfil + índice de username.
                return
            }

            is ApiResult.Err -> throw result.error.toUserMessageException()
        }
    }

    private fun ApiError.toUserMessageException(): IllegalStateException {
        val backendMessage = message.orEmpty().lowercase()
        return when {
            this is ApiError.FailedPrecondition && "username_taken" in backendMessage -> {
                IllegalStateException("Ese username ya está en uso")
            }

            this is ApiError.InvalidArgument -> {
                IllegalStateException(message ?: "Username inválido")
            }

            else -> IllegalStateException(message ?: "No se pudo guardar el perfil")
        }
    }

    private companion object {
        const val SAVE_USER_PROFILE_CALLABLE = "saveUserProfile"
    }
}

@Serializable
data class UserProfileRemoteDto(
    val displayName: String? = null,
    val photoUrl: String? = null,
    val email: String? = null,
    val username: String? = null,
    val birthDate: LocalDate? = null,
    val zodiacSign: ZodiacSign? = null,
    val birthEssenceSummary: String? = null,
    val updatedAtEpochMillis: Long
) {
    fun toUserProfile(): UserProfile =
        UserProfile(
            displayName = displayName,
            photoUrl = photoUrl,
            email = email,
            username = username,
            birthDate = birthDate,
            zodiacSign = zodiacSign,
            birthEssenceSummary = birthEssenceSummary
        )

    companion object {
        fun fromUserProfile(profile: UserProfile, updatedAtEpochMillis: Long) =
            UserProfileRemoteDto(
                displayName = profile.displayName,
                photoUrl = profile.photoUrl,
                email = profile.email,
                username = profile.username,
                birthDate = profile.birthDate,
                zodiacSign = profile.zodiacSign,
                birthEssenceSummary = profile.birthEssenceSummary,
                updatedAtEpochMillis = updatedAtEpochMillis
            )
    }
}
