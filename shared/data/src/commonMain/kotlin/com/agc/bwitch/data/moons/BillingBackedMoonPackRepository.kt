package com.agc.bwitch.data.moons

import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.MoonPackProductStatus
import com.agc.bwitch.domain.moons.MoonPackRepository

class BillingBackedMoonPackRepository(
    private val billing: MoonPackBillingDataSource,
) : MoonPackRepository {
    override suspend fun getMoonPacks(): List<MoonPack> {
        val catalog = listOf(
            MoonPack("bwitch_moons_pack_10", 10, "Starter", null, 1, MoonPackProductStatus.Loading),
            MoonPack("bwitch_moons_pack_30", 30, "Mystic", null, 2, MoonPackProductStatus.Loading),
            MoonPack("bwitch_moons_pack_80", 80, "Coven", null, 3, MoonPackProductStatus.Loading),
        )
        if (!billing.isSupported) return catalog.map { it.copy(status = MoonPackProductStatus.Unavailable) }
        val details = runCatching { billing.queryMoonPackProductDetails().associateBy { it.productId } }.getOrDefault(emptyMap())
        return catalog.map { pack ->
            val d = details[pack.productId]
            if (d != null) pack.copy(localizedPrice = d.localizedPrice, status = MoonPackProductStatus.Available)
            else pack.copy(status = MoonPackProductStatus.Unavailable)
        }
    }
}
