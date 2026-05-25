package com.agc.bwitch.data.moons

import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.MoonPackProductStatus
import com.agc.bwitch.domain.moons.MoonPackRepository

class MockMoonPackRepository : MoonPackRepository {
    override suspend fun getMoonPacks(): List<MoonPack> = mockPacks.sortedBy { it.displayOrder }

    private companion object {
        val mockPacks = listOf(
            MoonPack(
                productId = "bwitch_moons_pack_10",
                moonAmount = 10,
                label = "Starter",
                localizedPrice = null,
                displayOrder = 1,
                status = MoonPackProductStatus.Unavailable,
            ),
            MoonPack(
                productId = "bwitch_moons_pack_30",
                moonAmount = 30,
                label = "Mystic",
                localizedPrice = null,
                displayOrder = 2,
                status = MoonPackProductStatus.Unavailable,
            ),
            MoonPack(
                productId = "bwitch_moons_pack_80",
                moonAmount = 80,
                label = "Coven",
                localizedPrice = null,
                displayOrder = 3,
                status = MoonPackProductStatus.Unavailable,
            ),
        )
    }
}
