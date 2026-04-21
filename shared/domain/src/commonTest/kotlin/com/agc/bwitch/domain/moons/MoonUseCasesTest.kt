package com.agc.bwitch.domain.moons

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

class MoonUseCasesTest {

    @Test
    fun `spend moons updates balance when there is enough`() = runTest {
        val repository = InMemoryMoonRepository(initialBalance = 7)
        val spendMoons = SpendMoonsUseCase(repository)

        val result = spendMoons(3)

        val success = result as SpendMoonsResult.Success
        assertEquals(4, success.updatedBalance.amount)
        assertEquals(4, repository.getBalance().amount)
    }

    @Test
    fun `spend moons returns insufficient when balance is not enough`() = runTest {
        val repository = InMemoryMoonRepository(initialBalance = 1)
        val spendMoons = SpendMoonsUseCase(repository)

        val result = spendMoons(3)

        val insufficient = result as SpendMoonsResult.InsufficientBalance
        assertEquals(1, insufficient.currentBalance.amount)
        assertEquals(3, insufficient.required)
        assertEquals(1, repository.getBalance().amount)
    }

    @Test
    fun `has enough moons validates available balance`() = runTest {
        val repository = InMemoryMoonRepository(initialBalance = 5)
        val hasEnoughMoons = HasEnoughMoonsUseCase(repository)

        assertTrue(hasEnoughMoons(5))
    }

    private class InMemoryMoonRepository(initialBalance: Int) : MoonRepository {
        private val state = MutableStateFlow(MoonBalance(initialBalance))

        override suspend fun getBalance(): MoonBalance = state.value

        override fun observeBalance(): Flow<MoonBalance> = state

        override suspend fun addMoons(amount: Int): MoonBalance {
            val updated = MoonBalance(state.value.amount + amount)
            state.value = updated
            return updated
        }

        override suspend fun spendMoons(amount: Int): SpendMoonsResult {
            val current = state.value
            if (current.amount < amount) {
                return SpendMoonsResult.InsufficientBalance(currentBalance = current, required = amount)
            }
            val updated = MoonBalance(current.amount - amount)
            state.value = updated
            return SpendMoonsResult.Success(updated)
        }

        override suspend fun hasEnough(amount: Int): Boolean = state.value.amount >= amount
    }
}
