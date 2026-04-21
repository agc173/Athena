package com.agc.bwitch.data.moons

import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.MoonPackRepository

class MockMoonPackRepository : MoonPackRepository {
    override suspend fun getMoonPacks(): List<MoonPack> = mockPacks
        .sortedBy { it.displayOrder }

    private companion object {
        val mockPacks = listOf(
            MoonPack(
                id = "starter",
                moons = 10,
                label = "Starter",
                displayPrice = "$0.99",
                displayOrder = 1,
            ),
            MoonPack(
                id = "mystic",
                moons = 30,
                label = "Mystic",
                displayPrice = "$2.99",
                displayOrder = 2,
            ),
            MoonPack(
                id = "coven",
                moons = 80,
                label = "Coven",
                displayPrice = "$6.99",
                displayOrder = 3,
            ),
        )
    }
}
