package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.agc.bwitch.domain.auth.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

class SyncBirthChartRepository(
    private val local: SettingsBirthChartRepository,
    private val authRepository: AuthRepository
) : BirthChartRepository {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun getBirthData(): BirthData? {
        val localValue = local.getBirthData()
        scope.launch { syncPullAndMaybeMerge() }
        return localValue
    }

    override fun observeBirthData() = local.observeBirthData()

    override suspend fun saveBirthData(data: BirthData) {
        // guarda local con updatedAt NOW
        local.saveBirthData(data)

        val updatedAt = local.getLocalUpdatedAtEpochMillisOrNull()
            ?: Clock.System.now().toEpochMilliseconds()

        scope.launch { pushLocalToRemote(data, updatedAt) }
    }

    /**
     * Útil para disparar tras login o refresh manual.
     */
    fun pull() {
        scope.launch { syncPullAndMaybeMerge() }
    }

    private suspend fun syncPullAndMaybeMerge() {
        val uid = currentUidOrNull() ?: return

        val remote = runCatching { fetchRemote(uid) }.getOrNull() ?: return
        val remoteData = remote.toBirthData()

        // re-leer local para comparar con el estado real actual
        val localValue = local.getBirthData()
        val localUpdated = local.getLocalUpdatedAtEpochMillisOrNull()
        val remoteUpdated = remote.updatedAtEpochMillis

        when {
            localValue == null -> {
                saveRemoteAsLocal(remoteData, remoteUpdated)
            }

            localUpdated == null -> {
                // estado raro (o V1 sin timestamp): normalizamos y pusheamos local
                val now = Clock.System.now().toEpochMilliseconds()
                local.saveBirthDataWithUpdatedAt(localValue, now)
                pushLocalToRemote(localValue, now)
            }

            remoteUpdated > localUpdated -> {
                saveRemoteAsLocal(remoteData, remoteUpdated)
            }

            remoteUpdated < localUpdated -> {
                pushLocalToRemote(localValue, localUpdated)
            }

            else -> {
                // iguales -> no-op
            }
        }
    }

    private suspend fun pushLocalToRemote(data: BirthData, updatedAtEpochMillis: Long) {
        val uid = currentUidOrNull() ?: return
        val dto = BirthDataRemoteDto.fromBirthData(data, updatedAtEpochMillis)
        runCatching { setRemote(uid, dto) }
            .onFailure { println("BirthChart push failed: ${it.message}") }
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

    private suspend fun saveRemoteAsLocal(data: BirthData, updatedAtEpochMillis: Long) {
        local.saveBirthDataWithUpdatedAt(data, updatedAtEpochMillis)
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

