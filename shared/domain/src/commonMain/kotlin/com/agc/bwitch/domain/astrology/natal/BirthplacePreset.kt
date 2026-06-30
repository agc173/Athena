package com.agc.bwitch.domain.astrology.natal

import kotlinx.serialization.Serializable

@Serializable
data class BirthplacePreset(
    val id: String,
    val cityName: String,
    val countryName: String,
    val latitudeDegrees: Double,
    val longitudeDegrees: Double,
    val timezoneId: String,
    val countryCode: String? = null,
    val searchNames: List<String> = emptyList(),
)
