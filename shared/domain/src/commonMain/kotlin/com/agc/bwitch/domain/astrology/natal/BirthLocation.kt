package com.agc.bwitch.domain.astrology.natal

import kotlinx.serialization.Serializable

/**
 * Geographic birth location in decimal degrees.
 *
 * Latitude is positive north of the equator; longitude is positive east of Greenwich.
 */
@Serializable
data class BirthLocation(
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
)
