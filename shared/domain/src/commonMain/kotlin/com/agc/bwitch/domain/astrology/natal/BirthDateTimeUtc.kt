package com.agc.bwitch.domain.astrology.natal

import kotlinx.serialization.Serializable

/**
 * Birth date and time normalized to UTC before passing it to Astronomy Engine.
 */
@Serializable
data class BirthDateTimeUtc(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Double = 0.0,
)
