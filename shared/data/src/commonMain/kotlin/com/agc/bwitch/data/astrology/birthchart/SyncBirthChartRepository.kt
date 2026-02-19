package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.agc.bwitch.domain.auth.AuthRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

class SyncBirthChartRepository(
    private val local: BirthChartRepository, // tu SettingsBirthChartRepository
    private val authRepository: AuthRepository
) : BirthChartRepository {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override suspend fun getBirthData(): BirthData? {
        val localValue = local.getBirthData()

        // Sync en background (no bloquea la UI)
        scope.launch { syncPullAndMaybeMerge(localValue) }

        return localValue
    }

    override suspend fun saveBirthData(data: BirthData) {
        local.saveBirthData(data)

        val updatedAt = readLocalUpdatedAtEpochMillisOrNull()
            ?: Clock.System.now().toEpochMilliseconds()

        scope.launch { pushLocalToRemote(data, updatedAt) }
    }


    override fun observeBirthData() = local.observeBirthData()


    private suspend fun syncPullAndMaybeMerge(localValue: BirthData?) {
        val uid = currentUidOrNull() ?: return

        val remote = runCatching { fetchRemote(uid) }.getOrNull() ?: return
        val remoteData = remote.toBirthData()

        // Si no quieres conflictos por campos, hacemos replace por updatedAt
        // Como tu domain no tiene meta, usamos esta regla simple:
        // - si local es null -> guardar remoto en local
        // - si local existe -> si remoto es "más nuevo" que el local guardado en Settings V2
        // Para esto, lo correcto es guardar updatedAt también en Settings (paso 3)
        val localUpdated = readLocalUpdatedAtEpochMillisOrNull()
        val remoteUpdated = remote.updatedAtEpochMillis

        when {
            localValue == null -> saveRemoteAsLocal(remoteData, remote.updatedAtEpochMillis)
            localUpdated == null -> { /* si aún estás en V1, no machacamos; o migramos */ }
            remoteUpdated > localUpdated -> saveRemoteAsLocal(remoteData, remote.updatedAtEpochMillis)
            remoteUpdated < localUpdated -> pushLocalToRemote(localValue, localUpdated)

        }
    }

    private suspend fun pushLocalToRemote(data: BirthData, updatedAtEpochMillis: Long) {
        val uid = currentUidOrNull() ?: return
        val dto = BirthDataRemoteDto.fromBirthData(data, updatedAtEpochMillis = updatedAtEpochMillis)
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
        val settingsRepo = local as? SettingsBirthChartRepository
        if (settingsRepo != null) {
            settingsRepo.saveBirthDataWithUpdatedAt(data, updatedAtEpochMillis)
        } else {
            // fallback si algún día cambias el local repo
            local.saveBirthData(data)
        }
    }


    private fun readLocalUpdatedAtEpochMillisOrNull(): Long? =
        (local as? SettingsBirthChartRepository)?.getLocalUpdatedAtEpochMillisOrNull()



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

