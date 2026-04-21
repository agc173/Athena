package com.agc.bwitch.domain.moons

import kotlinx.coroutines.flow.Flow

class GetMoonBalanceUseCase(
    private val repository: MoonRepository,
) {
    suspend operator fun invoke(): MoonBalance = repository.getBalance()
}

class ObserveMoonBalanceUseCase(
    private val repository: MoonRepository,
) {
    operator fun invoke(): Flow<MoonBalance> = repository.observeBalance()
}

class AddMoonsUseCase(
    private val repository: MoonRepository,
) {
    suspend operator fun invoke(amount: Int): MoonBalance = repository.addMoons(amount)
}

class SpendMoonsUseCase(
    private val repository: MoonRepository,
) {
    suspend operator fun invoke(amount: Int): SpendMoonsResult = repository.spendMoons(amount)
}

class HasEnoughMoonsUseCase(
    private val repository: MoonRepository,
) {
    suspend operator fun invoke(amount: Int): Boolean = repository.hasEnough(amount)
}

class GetMoonPacksUseCase(
    private val repository: MoonPackRepository,
) {
    suspend operator fun invoke(): List<MoonPack> = repository.getMoonPacks()
}
