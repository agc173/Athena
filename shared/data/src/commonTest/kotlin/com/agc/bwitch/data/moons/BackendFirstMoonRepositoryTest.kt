package com.agc.bwitch.data.moons

import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus
import com.agc.bwitch.domain.economy.PendulumAuthorizationResult
import com.agc.bwitch.domain.economy.SynastryAuthorizationResult
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.SpendMoonsResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BackendFirstMoonRepositoryTest {

    @Test
    fun `getBalance prefers backend when available`() = runBlocking {
        val local = FakeMoonRepository(initialBalance = 4)
        val economy = FakeEconomyRepository(statusBalance = 9)
        val repository = BackendFirstMoonRepository(localRepository = local, economyRepository = economy)

        val result = repository.getBalance()

        assertEquals(9, result.amount)
    }

    @Test
    fun `getBalance falls back to local when backend fails`() = runBlocking {
        val local = FakeMoonRepository(initialBalance = 4)
        val economy = FakeEconomyRepository(
            statusError = IllegalStateException("status down"),
            balanceError = IllegalStateException("balance down"),
        )
        val repository = BackendFirstMoonRepository(localRepository = local, economyRepository = economy)

        val result = repository.getBalance()

        assertEquals(4, result.amount)
    }

    @Test
    fun `getBalance uses backend getBalance when status fails`() = runBlocking {
        val local = FakeMoonRepository(initialBalance = 4)
        val economy = FakeEconomyRepository(
            statusError = IllegalStateException("status down"),
            balanceValue = 7,
        )
        val repository = BackendFirstMoonRepository(localRepository = local, economyRepository = economy)

        val result = repository.getBalance()

        assertEquals(7, result.amount)
    }

    @Test
    fun `observeBalance emits synced backend snapshot first`() = runBlocking {
        val local = FakeMoonRepository(initialBalance = 2)
        val economy = FakeEconomyRepository(statusBalance = 11)
        val repository = BackendFirstMoonRepository(localRepository = local, economyRepository = economy)

        val first = repository.observeBalance().first()

        assertEquals(11, first.amount)
    }

    private class FakeMoonRepository(initialBalance: Int) : MoonRepository {
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
            return if (current.amount < amount) {
                SpendMoonsResult.InsufficientBalance(currentBalance = current, required = amount)
            } else {
                val updated = MoonBalance(current.amount - amount)
                state.value = updated
                SpendMoonsResult.Success(updated)
            }
        }

        override suspend fun hasEnough(amount: Int): Boolean = state.value.amount >= amount
    }

    private class FakeEconomyRepository(
        private val statusBalance: Int? = null,
        private val balanceValue: Int? = null,
        private val statusError: Throwable? = null,
        private val balanceError: Throwable? = null,
    ) : EconomyRepository {
        override suspend fun getBalance(): EconomyBalance {
            balanceError?.let { throw it }
            return EconomyBalance(
                balance = balanceValue ?: statusBalance ?: 0,
                dailyLoginClaimed = false,
                rewardedAdsClaimed = 0,
                rewardedAdsRemaining = 0,
            )
        }

        override suspend fun getStatus(): EconomyStatus {
            statusError?.let { throw it }
            return EconomyStatus(
                balance = statusBalance ?: 0,
                isPremium = false,
                todayDateIso = "2026-04-23",
            )
        }

        override suspend fun claimDailyLogin(requestId: String): EconomyClaimResult = EconomyClaimResult(
            result = EconomyClaimStatus.ALREADY_CLAIMED,
            balance = statusBalance ?: 0,
            dailyLoginClaimed = true,
            rewardedAdsClaimed = 0,
            rewardedAdsRemaining = 0,
        )

        override suspend fun claimRewardedAd(
            requestId: String,
            adProof: String,
            placement: String?,
        ): EconomyClaimResult = EconomyClaimResult(
            result = EconomyClaimStatus.DAILY_LIMIT_REACHED,
            balance = statusBalance ?: 0,
            dailyLoginClaimed = false,
            rewardedAdsClaimed = 0,
            rewardedAdsRemaining = 0,
        )

        override suspend fun getModulePreviews(modules: List<String>): List<EconomyModulePreview> = emptyList()

        override suspend fun authorizeSynastry(
            requestId: String,
            languageCode: String?,
        ): SynastryAuthorizationResult {
            return SynastryAuthorizationResult(authorized = true, economyDisabled = true)
        }

        override suspend fun authorizePendulum(
            requestId: String,
            languageCode: String?,
        ): PendulumAuthorizationResult {
            return PendulumAuthorizationResult(authorized = true, economyDisabled = true)
        }
    }
}
