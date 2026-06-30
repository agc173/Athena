package com.agc.bwitch.domain.astrology.natal

import kotlinx.serialization.Serializable

@Serializable
data class NatalChartResult(
    val sunLongitudeDegrees: Double,
    val sunSign: ZodiacSign,
    val moonLongitudeDegrees: Double,
    val moonSign: ZodiacSign,
    val ascendantLongitudeDegrees: Double? = null,
    val ascendantSign: ZodiacSign? = null,
)
