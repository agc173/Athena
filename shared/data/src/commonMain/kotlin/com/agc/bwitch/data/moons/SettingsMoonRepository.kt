package com.agc.bwitch.data.moons

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.SpendMoonsResult
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsMoonRepository(
    settingsFactory: SettingsFactory,
) : MoonRepository {

    private val settings: Settings = settingsFactory.create(SETTINGS_NAME)
    private val state = MutableStateFlow(readBalance())

    override suspend fun getBalance(): MoonBalance = state.value

    override fun observeBalance(): Flow<MoonBalance> = state

    override suspend fun addMoons(amount: Int): MoonBalance {
        if (amount <= 0) return state.value
        val updated = MoonBalance(amount = state.value.amount + amount)
        persist(updated)
        state.value = updated
        return updated
    }

    override suspend fun spendMoons(amount: Int): SpendMoonsResult {
        if (amount <= 0) return SpendMoonsResult.Success(state.value)

        val current = state.value
        if (current.amount < amount) {
            return SpendMoonsResult.InsufficientBalance(
                currentBalance = current,
                required = amount,
            )
        }

        val updated = MoonBalance(amount = current.amount - amount)
        persist(updated)
        state.value = updated
        return SpendMoonsResult.Success(updated)
    }

    override suspend fun hasEnough(amount: Int): Boolean = state.value.amount >= amount

    private fun readBalance(): MoonBalance = MoonBalance(
        amount = settings.getInt(BALANCE_KEY, DEFAULT_BALANCE),
    )

    private fun persist(balance: MoonBalance) {
        settings.putInt(BALANCE_KEY, balance.amount)
    }

    private companion object {
        private const val SETTINGS_NAME = "bwitch_moons"
        private const val BALANCE_KEY = "balance"
        private const val DEFAULT_BALANCE = 0
    }
}
