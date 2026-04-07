package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.sync.LocalStore
import com.agc.bwitch.data.sync.RemoteStore
import com.agc.bwitch.data.sync.SyncEngine
import com.agc.bwitch.data.sync.Timestamped
import com.agc.bwitch.domain.rituals.DailyHabitsState
import com.agc.bwitch.domain.rituals.HabitIntention
import com.agc.bwitch.domain.rituals.HabitsProgress
import com.agc.bwitch.domain.rituals.HabitsRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class SyncHabitsRepository(
    private val localRepo: SettingsHabitsRepository,
    private val firestore: FirebaseFirestore,
) : HabitsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val localStore = object : LocalStore<HabitsLocalState> {
        override fun observe() = kotlinx.coroutines.flow.flowOf(localRepo.getStateSnapshot())
        override suspend fun get(): HabitsLocalState = localRepo.getStateSnapshot()

        override suspend fun save(value: HabitsLocalState, updatedAtEpochMillis: Long) {
            localRepo.saveStateSnapshot(value.copy(updatedAtEpochMillis = updatedAtEpochMillis))
        }

        override fun localUpdatedAtEpochMillisOrNull(): Long? = localRepo.getLocalUpdatedAtEpochMillisOrNull()
    }

    private val remoteStore = object : RemoteStore<HabitsLocalState> {
        override suspend fun fetch(): Timestamped<HabitsLocalState> {
            val uid = currentUidOrNull() ?: return Timestamped(null, null)
            val snap = doc(uid).get()
            if (!snap.exists) return Timestamped(null, null)

            val dto = snap.data(HabitsRemoteDto.serializer())
            return Timestamped(dto.toLocalState(), dto.updatedAtEpochMillis)
        }

        override suspend fun push(value: HabitsLocalState, updatedAtEpochMillis: Long) {
            val uid = currentUidOrNull() ?: return
            val payload = HabitsRemoteDto.fromLocalState(value.copy(updatedAtEpochMillis = updatedAtEpochMillis))
            runCatching { doc(uid).set(payload) }
                .onFailure { error ->
                    println("BWITCH_HABITS_SYNC push failed uid=$uid reason=${error.message}")
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

    override fun getTodayIntentions(): List<HabitIntention> = withPullAndPushIfNeeded {
        localRepo.getTodayIntentions()
    }

    override fun getTodayState(): DailyHabitsState = withPullAndPushIfNeeded {
        localRepo.getTodayState()
    }

    override fun getProgress(): HabitsProgress {
        val before = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        val progress = localRepo.getProgress()
        val after = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        if (after != null && after != before) {
            pushLocalState(after)
        }
        engine.pullAsync()
        return progress
    }

    override fun markCompleted(intentionId: String) {
        pullSyncShort()
        localRepo.markCompleted(intentionId)
        localRepo.getLocalUpdatedAtEpochMillisOrNull()?.let(::pushLocalState)
    }

    override fun unmarkCompleted(intentionId: String) {
        pullSyncShort()
        localRepo.unmarkCompleted(intentionId)
        localRepo.getLocalUpdatedAtEpochMillisOrNull()?.let(::pushLocalState)
    }

    private fun <T> withPullAndPushIfNeeded(action: () -> T): T {
        pullSyncShort()
        val before = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        val result = action()
        val after = localRepo.getLocalUpdatedAtEpochMillisOrNull()
        if (after != null && after != before) {
            pushLocalState(after)
        }
        return result
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
            .collection("habits")
            .document("current")

    private fun currentUidOrNull(): String? = Firebase.auth.currentUser?.uid
}
