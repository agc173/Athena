package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.sync.LocalStore
import com.agc.bwitch.data.sync.RemoteStore
import com.agc.bwitch.data.sync.SyncEngine
import com.agc.bwitch.data.sync.Timestamped
import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDate

class SyncDailyRitualRepository(
    private val localRepo: SettingsDailyRitualRepository,
    private val firestore: FirebaseFirestore,
) : DailyRitualRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localStore = object : LocalStore<DailyRitualLocalState> {
        override fun observe() = kotlinx.coroutines.flow.flowOf(localRepo.getStateSnapshot())
        override suspend fun get(): DailyRitualLocalState = localRepo.getStateSnapshot()

        override suspend fun save(value: DailyRitualLocalState, updatedAtEpochMillis: Long) {
            localRepo.saveStateSnapshot(value.copy(updatedAtEpochMillis = updatedAtEpochMillis))
        }

        override fun localUpdatedAtEpochMillisOrNull(): Long? = localRepo.getLocalUpdatedAtEpochMillisOrNull()
    }

    private val remoteStore = object : RemoteStore<DailyRitualLocalState> {
        override suspend fun fetch(): Timestamped<DailyRitualLocalState> {
            val uid = currentUidOrNull() ?: return Timestamped(null, null)
            val snap = doc(uid).get()
            if (!snap.exists) return Timestamped(null, null)

            val dto = snap.data(DailyRitualRemoteDto.serializer())
            return Timestamped(dto.toLocalState(), dto.updatedAtEpochMillis)
        }

        override suspend fun push(value: DailyRitualLocalState, updatedAtEpochMillis: Long) {
            val uid = currentUidOrNull() ?: return
            val payload = DailyRitualRemoteDto.fromLocalState(value.copy(updatedAtEpochMillis = updatedAtEpochMillis))
            runCatching { doc(uid).set(payload) }
                .onFailure { error ->
                    println("BWITCH_DAILY_RITUAL_SYNC push failed uid=$uid reason=${error.message}")
                }
        }
    }

    private val engine = SyncEngine(
        scope = scope,
        local = localStore,
        remote = remoteStore,
    )

    init {
        engine.pullAsync()
    }

    override fun getTemplateForDate(date: LocalDate): DailyRitualTemplate {
        pullSyncShort()

        val before = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        val ritual = localRepo.getTemplateForDate(date)
        val after = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        if (after != null && after != before) {
            pushLocalState(after)
        }
        return ritual
    }

    override fun isCompletedOn(date: LocalDate): Boolean {
        return localRepo.isCompletedOn(date)
    }

    override fun getStreakForDate(date: LocalDate): Int {
        return localRepo.getStreakForDate(date)
    }

    override fun completeOn(date: LocalDate): Int {
        pullSyncShort()

        val streak = localRepo.completeOn(date)
        localRepo.getLocalUpdatedAtEpochMillisOrNull()?.let(::pushLocalState)
        return streak
    }


    private fun pullSyncShort() {
        runBlocking {
            withTimeoutOrNull(750) {
                runCatching { engine.pull() }
            }
        }
    }

    private fun pushLocalState(updatedAtEpochMillis: Long) {
        scope.launch {
            val localState = localRepo.getStateSnapshot()
            runCatching { remoteStore.push(localState, updatedAtEpochMillis) }
        }
    }

    private fun doc(uid: String) =
        firestore.collection("users")
            .document(uid)
            .collection("dailyRitual")
            .document("current")

    private fun currentUidOrNull(): String? = Firebase.auth.currentUser?.uid
}
