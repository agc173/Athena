package com.agc.bwitch.data.moons.billing.googleplay

object GooglePlayMoonPackProducts {
    const val PACK_10 = "bwitch_moons_pack_10"
    const val PACK_30 = "bwitch_moons_pack_30"
    const val PACK_80 = "bwitch_moons_pack_80"

    val queryOrder: List<String> = listOf(PACK_10, PACK_30, PACK_80)
    val knownProducts: Set<String> = queryOrder.toSet()
}
