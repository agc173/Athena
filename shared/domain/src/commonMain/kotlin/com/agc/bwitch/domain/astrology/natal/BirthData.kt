package com.agc.bwitch.domain.astrology.natal

import kotlinx.serialization.Serializable

@Serializable
data class BirthData(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Double = 0.0,
)
