package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.data.sync.LocalStore
import com.agc.bwitch.data.sync.RemoteStore
import com.agc.bwitch.data.sync.SyncEngine
import com.agc.bwitch.data.sync.Timestamped
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthChartSyncController
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceReading
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import com.agc.bwitch.domain.shared.ApiResult
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

class SyncBirthChartRepository(
    private val localRepo: SettingsBirthChartRepository,
    private val authRepository: AuthRepository,
    private val functionsClient: FunctionsClient,
    private val userProfileRepository: UserProfileRepository,
) : BirthChartRepository, BirthChartSyncController {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localStore = object : LocalStore<BirthEssenceProfile> {
        override fun observe() = localRepo.observeBirthEssence()
        override suspend fun get() = localRepo.getBirthEssence()
        override suspend fun save(value: BirthEssenceProfile, updatedAtEpochMillis: Long) {
            localRepo.saveBirthEssenceWithTimestamps(
                draft = value.toDraft(),
                savedAtEpochMillis = value.savedAtEpochMillis,
                updatedAtEpochMillis = updatedAtEpochMillis,
            )
        }

        override fun localUpdatedAtEpochMillisOrNull(): Long? = localRepo.getLocalUpdatedAtEpochMillisOrNull()
    }

    private val remoteStore = object : RemoteStore<BirthEssenceProfile> {
        override suspend fun fetch(): Timestamped<BirthEssenceProfile> {
            val uid = currentUidOrNull() ?: return Timestamped(null, null)
            val dto = fetchRemote(uid) ?: return Timestamped(null, null)
            return Timestamped(dto.toDomain(), dto.updatedAtEpochMillis)
        }

        override suspend fun push(value: BirthEssenceProfile, updatedAtEpochMillis: Long) {
            val uid = currentUidOrNull() ?: run {
                println("BWITCH_BIRTH_SYNC push skipped: missing uid")
                return
            }
            val dto = BirthEssenceRemoteDto.fromDomain(value.copy(updatedAtEpochMillis = updatedAtEpochMillis))
            runCatching { setRemote(uid, dto) }
                .onSuccess {
                    println("BWITCH_BIRTH_SYNC remote write ok uid=$uid updatedAt=$updatedAtEpochMillis")
                }
                .onFailure { error ->
                    println("BWITCH_BIRTH_SYNC remote write failed uid=$uid reason=${error.message}")
                }
        }
    }

    private val engine = SyncEngine(
        scope = scope,
        local = localStore,
        remote = remoteStore
    )

    override fun observeBirthEssence() = engine.observe()

    override suspend fun getBirthEssence(): BirthEssenceProfile? = engine.get()

    override suspend fun saveBirthEssence(draft: BirthEssenceDraft) {
        val current = engine.get()
        val now = Clock.System.now().toEpochMilliseconds()
        val profile = BirthEssenceProfile(
            sunSign = draft.sunSign,
            moonSign = draft.moonSign,
            risingSign = draft.risingSign,
            interpretation = draft.interpretation,
            archetype = draft.archetype,
            savedAtEpochMillis = current?.savedAtEpochMillis ?: now,
            updatedAtEpochMillis = now,
        )

        println("BWITCH_BIRTH_SYNC save requested updatedAt=$now")
        engine.save(profile, now)
        println("BWITCH_BIRTH_SYNC save local persisted; remote push delegated to engine")
        linkEssenceSummaryToProfile(profile)
    }

    override suspend fun generateBirthEssence(input: BirthEssenceInput): ApiResult<BirthEssenceReading> {
        return when (
            val result = functionsClient.call(
                name = "birthEssenceGenerate",
                data = BirthEssenceGenerateRequestDto(
                    sunSign = input.sunSign.name.uppercase(),
                    moonSign = input.moonSign.name.uppercase(),
                    risingSign = input.risingSign.name.uppercase(),
                    archetype = input.archetypeHint?.name,
                ),
                requestSerializer = BirthEssenceGenerateRequestDto.serializer(),
                responseSerializer = BirthEssenceGenerateResponseDto.serializer(),
            )
        ) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> {
                ApiResult.Ok(
                    BirthEssenceReading(
                        interpretation = result.value.interpretation,
                        archetype = null,
                    )
                )
            }
        }
    }

    override suspend fun pull() {
        engine.pull()
        val synced = localRepo.getBirthEssence()
        if (synced == null) {
            println("BWITCH_BIRTH_SYNC pull completed: no remote essence")
            return
        }

        println("BWITCH_BIRTH_SYNC pull completed updatedAt=${synced.updatedAtEpochMillis}")
        linkEssenceSummaryToProfile(synced)
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
            .collection("birthEssence")
            .document("current")

    private suspend fun fetchRemote(uid: String): BirthEssenceRemoteDto? {
        val snap = doc(uid).get()
        if (!snap.exists) return null
        return snap.data(BirthEssenceRemoteDto.serializer())
    }

    private suspend fun setRemote(uid: String, dto: BirthEssenceRemoteDto) {
        doc(uid).set(dto)
    }

    private suspend fun linkEssenceSummaryToProfile(essence: BirthEssenceProfile) {
        val currentProfile = userProfileRepository.getUserProfile()
        if (currentProfile == null) {
            println("BWITCH_BIRTH_SYNC profile summary skipped: profile unavailable")
            return
        }
        val summary = "${essence.sunSign.label} · ${essence.moonSign.label} · ${essence.risingSign.label}"
        if (currentProfile.birthEssenceSummary == summary) {
            println("BWITCH_BIRTH_SYNC profile summary unchanged")
            return
        }

        runCatching {
            userProfileRepository.saveUserProfile(
                currentProfile.copy(birthEssenceSummary = summary)
            )
        }.onSuccess {
            println("BWITCH_BIRTH_SYNC profile summary updated: $summary")
        }.onFailure { error ->
            println("BWITCH_BIRTH_SYNC profile summary update failed: ${error.message}")
        }
    }

    private fun BirthEssenceProfile.toDraft(): BirthEssenceDraft =
        BirthEssenceDraft(
            sunSign = sunSign,
            moonSign = moonSign,
            risingSign = risingSign,
            interpretation = interpretation,
            archetype = archetype,
        )
}

@Serializable
data class BirthEssenceRemoteDto(
    val sunSign: String,
    val moonSign: String,
    val risingSign: String,
    val interpretation: String,
    val archetype: String? = null,
    val savedAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
) {
    fun toDomain(): BirthEssenceProfile =
        BirthEssenceProfile(
            sunSign = com.agc.bwitch.domain.astrology.horoscope.ZodiacSign.valueOf(sunSign.lowercase()),
            moonSign = com.agc.bwitch.domain.astrology.horoscope.ZodiacSign.valueOf(moonSign.lowercase()),
            risingSign = com.agc.bwitch.domain.astrology.horoscope.ZodiacSign.valueOf(risingSign.lowercase()),
            interpretation = interpretation,
            archetype = BirthEssenceArchetype.fromRawOrNull(archetype),
            savedAtEpochMillis = savedAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )

    companion object {
        fun fromDomain(data: BirthEssenceProfile) =
            BirthEssenceRemoteDto(
                sunSign = data.sunSign.name,
                moonSign = data.moonSign.name,
                risingSign = data.risingSign.name,
                interpretation = data.interpretation,
                archetype = data.archetype?.name,
                savedAtEpochMillis = data.savedAtEpochMillis,
                updatedAtEpochMillis = data.updatedAtEpochMillis,
            )
    }
}

@Serializable
data class BirthEssenceGenerateRequestDto(
    val sunSign: String,
    val moonSign: String,
    val risingSign: String,
    val archetype: String? = null,
)

@Serializable
data class BirthEssenceGenerateResponseDto(
    val interpretation: String,
    val archetype: String? = null,
)
