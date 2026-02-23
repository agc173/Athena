package com.agc.bwitch.data.userprofile

import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.Serializable
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class SyncUserProfileRepository(
    private val local: SettingsUserProfileRepository,
    private val authRepository: AuthRepository
) : UserProfileRepository {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun observeUserProfile() = local.observeUserProfile()

    override suspend fun getUserProfile(): UserProfile? {
        val localValue = local.getUserProfile()
        scope.launch { syncPullAndMaybeMerge() }
        return localValue
    }

    override suspend fun saveUserProfile(profile: UserProfile) {
        local.saveUserProfile(profile)

        val updatedAt = local.getLocalUpdatedAtEpochMillisOrNull()
            ?: Clock.System.now().toEpochMilliseconds()

        scope.launch { pushLocalToRemote(profile, updatedAt) }
    }

    fun pull() {
        scope.launch { syncPullAndMaybeMerge() }
    }

    private suspend fun syncPullAndMaybeMerge() {
        val uid = currentUidOrNull() ?: return

        val remote = runCatching { fetchRemote(uid) }.getOrNull() ?: return
        val remoteProfile = remote.toUserProfile()

        val localValue = local.getUserProfile()
        val localUpdated = local.getLocalUpdatedAtEpochMillisOrNull()
        val remoteUpdated = remote.updatedAtEpochMillis

        when {
            localValue == null -> {
                local.saveUserProfileWithUpdatedAt(remoteProfile, remoteUpdated)
            }

            localUpdated == null -> {
                val now = Clock.System.now().toEpochMilliseconds()
                local.saveUserProfileWithUpdatedAt(localValue, now)
                pushLocalToRemote(localValue, now)
            }

            remoteUpdated > localUpdated -> {
                local.saveUserProfileWithUpdatedAt(remoteProfile, remoteUpdated)
            }

            remoteUpdated < localUpdated -> {
                pushLocalToRemote(localValue, localUpdated)
            }

            else -> Unit
        }
    }

    private suspend fun pushLocalToRemote(profile: UserProfile, updatedAtEpochMillis: Long) {
        val uid = currentUidOrNull()
        println("UserProfile push uid=$uid updatedAt=$updatedAtEpochMillis profile=$profile")
        if (uid == null) return

        val dto = UserProfileRemoteDto.fromUserProfile(profile, updatedAtEpochMillis)
        runCatching { setRemote(uid, dto) }
            .onSuccess { println("UserProfile push OK -> users/$uid/profile/current") }
            .onFailure { it.printStackTrace() }
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