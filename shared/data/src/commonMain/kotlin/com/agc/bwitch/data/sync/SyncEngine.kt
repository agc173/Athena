package com.agc.bwitch.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

data class Timestamped<T>(
    val value: T?,
    val updatedAtEpochMillis: Long?
)

interface LocalStore<T> {
    fun observe(): kotlinx.coroutines.flow.Flow<T?>
    suspend fun get(): T?
    suspend fun save(value: T, updatedAtEpochMillis: Long)
    fun localUpdatedAtEpochMillisOrNull(): Long?
}

interface RemoteStore<T> {
    suspend fun fetch(): Timestamped<T> // value + updatedAt remotos
    suspend fun push(value: T, updatedAtEpochMillis: Long)
}

/**
 * Engine local-first:
 * - get(): devuelve local y dispara pull best-effort en background
 * - save(): guarda local con ts y hace push best-effort
 * - pull(): mergea por timestamp y aplica ganador
 */
class SyncEngine<T>(
    private val scope: CoroutineScope,
    private val local: LocalStore<T>,
    private val remote: RemoteStore<T>
) {
    fun observe() = local.observe()

    suspend fun get(): T? {
        val localValue = local.get()
        scope.launch { runCatching { pull() } }
        return localValue
    }

    suspend fun save(value: T, updatedAtEpochMillis: Long) {
        local.save(value, updatedAtEpochMillis)
        scope.launch { runCatching { remote.push(value, updatedAtEpochMillis) } }
    }

    fun pullAsync() {
        scope.launch { runCatching { pull() } }
    }

    suspend fun pull(): T? {
        val localValue = local.get()
        val localUpdated = local.localUpdatedAtEpochMillisOrNull()

        val remoteTs = runCatching { remote.fetch() }.getOrNull()
            ?: return localValue

        val remoteValue = remoteTs.value
        val remoteUpdated = remoteTs.updatedAtEpochMillis

        // Si no hay remoto, no hacemos nada (mantenemos local)
        if (remoteValue == null || remoteUpdated == null) return localValue

        // Reglas equivalentes a tus when actuales:
        return when {
            localValue == null -> {
                local.save(remoteValue, remoteUpdated)
                remoteValue
            }

            localUpdated == null -> {
                // Normaliza estado raro local sin ts: el caller decide el ts (aquí no inventamos)
                // Devolvemos local tal cual y NO empujamos aquí; eso se maneja en wrapper si lo necesitáis.
                localValue
            }

            remoteUpdated > localUpdated -> {
                local.save(remoteValue, remoteUpdated)
                remoteValue
            }

            remoteUpdated < localUpdated -> {
                // local gana -> empujar local a remoto
                runCatching { remote.push(localValue, localUpdated) }
                localValue
            }

            else -> localValue
        }
    }
}