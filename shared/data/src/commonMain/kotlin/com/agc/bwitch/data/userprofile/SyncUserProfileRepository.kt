package com.agc.bwitch.data.userprofile

import com.agc.bwitch.data.sync.LocalStore
import com.agc.bwitch.data.sync.RemoteStore
import com.agc.bwitch.data.sync.SyncEngine
import com.agc.bwitch.data.sync.Timestamped
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

class SyncUserProfileRepository(
    private val localRepo: SettingsUserProfileRepository,
    private val authRepository: AuthRepository
) : UserProfileRepository {

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
            val uid = currentUidOrNull() ?: return
            val dto = UserProfileRemoteDto.fromUserProfile(value, updatedAtEpochMillis)
            runCatching { setRemote(uid, dto) }
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
        // Guardas local con ts “now” (igual que antes, solo que ahora lo hacemos explícito)
        val now = Clock.System.now().toEpochMilliseconds()
        engine.save(profile, now)

        // Si por cualquier motivo local no tuviera ts (estado raro), lo normalizamos como hacías:
        val localUpdated = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        if (localUpdated == null) {
            val normalizedNow = Clock.System.now().toEpochMilliseconds()
            localRepo.saveUserProfileWithUpdatedAt(profile, normalizedNow)
            runCatching { remoteStore.push(profile, normalizedNow) }
        }
    }

    fun pull() = engine.pullAsync()

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

    private suspend fun setRemote(uid: String, dto: UserProfileRemoteDto) {
        doc(uid).set(dto)
    }
}

@Serializable
data class UserProfileRemoteDto(
    val displayName: String?,
    val photoUrl: String?,
    val email: String?,
    val updatedAtEpochMillis: Long
) {
    fun toUserProfile(): UserProfile =
        UserProfile(
            displayName = displayName,
            photoUrl = photoUrl,
            email = email
        )

    companion object {
        fun fromUserProfile(profile: UserProfile, updatedAtEpochMillis: Long) =
            UserProfileRemoteDto(
                displayName = profile.displayName,
                photoUrl = profile.photoUrl,
                email = profile.email,
                updatedAtEpochMillis = updatedAtEpochMillis
            )
    }
}