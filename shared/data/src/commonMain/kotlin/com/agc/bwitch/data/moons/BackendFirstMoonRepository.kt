package com.agc.bwitch.data.moons

import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.SpendMoonsResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow

/**
 * Política incremental de saldo:
 * - Source of truth preferido: backend economy.
 * - Fallback temporal: repositorio local legacy.
 *
 * Mantiene compatibilidad con flujos legacy (add/spend locales), pero consolida
 * lectura/observación hacia backend cuando está disponible.
 */
class BackendFirstMoonRepository(
    private val localRepository: MoonRepository,
    private val economyRepository: EconomyRepository,
) : MoonRepository {

    private val observedBalance = MutableStateFlow<MoonBalance?>(null)

    override suspend fun getBalance(): MoonBalance {
        val resolved = resolveBalance()
        observedBalance.value = resolved
        return resolved
    }

    override fun observeBalance(): Flow<MoonBalance> = flow {
        emit(getBalance())
        emitAll(observedBalance.filterNotNull().drop(1))
    }

    override suspend fun addMoons(amount: Int): MoonBalance {
        val updated = localRepository.addMoons(amount)
        observedBalance.value = updated
        return updated
    }

    override suspend fun spendMoons(amount: Int): SpendMoonsResult {
        val result = localRepository.spendMoons(amount)
        if (result is SpendMoonsResult.Success) {
            observedBalance.value = result.updatedBalance
        }
        return result
    }

    override suspend fun hasEnough(amount: Int): Boolean = getBalance().amount >= amount

    private suspend fun resolveBalance(): MoonBalance {
        val backendBalance = runCatching { economyRepository.getStatus().balance }
            .recoverCatching { economyRepository.getBalance().balance }
            .getOrNull()

        return if (backendBalance != null) {
            MoonBalance(backendBalance)
        } else {
            localRepository.getBalance()
        }
    }
}
