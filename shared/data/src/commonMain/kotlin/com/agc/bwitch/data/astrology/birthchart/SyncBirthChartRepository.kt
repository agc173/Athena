package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.data.sync.LocalStore
import com.agc.bwitch.data.sync.RemoteStore
import com.agc.bwitch.data.sync.SyncEngine
import com.agc.bwitch.data.sync.Timestamped
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.agc.bwitch.domain.auth.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import com.agc.bwitch.domain.astrology.birthchart.BirthChartSyncController

class SyncBirthChartRepository(
    private val localRepo: SettingsBirthChartRepository,
    private val authRepository: AuthRepository
) : BirthChartRepository, BirthChartSyncController {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localStore = object : LocalStore<BirthData> {
        override fun observe() = localRepo.observeBirthData()
        override suspend fun get() = localRepo.getBirthData()
        override suspend fun save(value: BirthData, updatedAtEpochMillis: Long) {
            localRepo.saveBirthDataWithUpdatedAt(value, updatedAtEpochMillis)
        }
        override fun localUpdatedAtEpochMillisOrNull(): Long? = localRepo.getLocalUpdatedAtEpochMillisOrNull()
    }

    private val remoteStore = object : RemoteStore<BirthData> {
        override suspend fun fetch(): Timestamped<BirthData> {
            val uid = currentUidOrNull() ?: return Timestamped(null, null)
            val dto = fetchRemote(uid) ?: return Timestamped(null, null)
            return Timestamped(dto.toBirthData(), dto.updatedAtEpochMillis)
        }

        override suspend fun push(value: BirthData, updatedAtEpochMillis: Long) {
            val uid = currentUidOrNull() ?: return
            val dto = BirthDataRemoteDto.fromBirthData(value, updatedAtEpochMillis)
            runCatching { setRemote(uid, dto) }
        }
    }

    private val engine = SyncEngine(
        scope = scope,
        local = localStore,
        remote = remoteStore
    )

    override fun observeBirthData() = engine.observe()

    override suspend fun getBirthData(): BirthData? = engine.get()

    override suspend fun saveBirthData(data: BirthData) {
        val now = Clock.System.now().toEpochMilliseconds()
        engine.save(data, now)

        // Normalización equivalente a tu caso localUpdated == null
        val localUpdated = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        if (localUpdated == null) {
            val normalizedNow = Clock.System.now().toEpochMilliseconds()
            localRepo.saveBirthDataWithUpdatedAt(data, normalizedNow)
            runCatching { remoteStore.push(data, normalizedNow) }
        }
    }

    override suspend fun pull() {
        engine.pull()
    }

    private suspend fun currentUidOrNull(): String? =
        authRepository.authState.first()?.uid

    private fun doc(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("birthChart")
            .document("current")

    private suspend fun fetchRemote(uid: String): BirthDataRemoteDto? {
        val snap = doc(uid).get()
        if (!snap.exists) return null
        return snap.data(BirthDataRemoteDto.serializer())
    }

    private suspend fun setRemote(uid: String, dto: BirthDataRemoteDto) {
        doc(uid).set(dto)
    }
}

@Serializable
data class BirthDataRemoteDto(
    val date: String,
    val time: String,
    val placeName: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val updatedAtEpochMillis: Long
) {
    fun toBirthData(): BirthData =
        BirthData(
            date = kotlinx.datetime.LocalDate.parse(date),
            time = kotlinx.datetime.LocalTime.parse(time),
            placeName = placeName,
            lat = lat,
            lon = lon
        )

    companion object {
        fun fromBirthData(data: BirthData, updatedAtEpochMillis: Long) =
            BirthDataRemoteDto(
                date = data.date.toString(),
                time = data.time.toString(),
                placeName = data.placeName,
                lat = data.lat,
                lon = data.lon,
                updatedAtEpochMillis = updatedAtEpochMillis
            )
    }
}

