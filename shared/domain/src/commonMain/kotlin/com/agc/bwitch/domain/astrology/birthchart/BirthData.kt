package com.agc.bwitch.domain.astrology.birthchart

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class BirthData(
    val date: LocalDate,
    val time: LocalTime,
    val placeName: String,
    val lat: Double? = null,
    val lon: Double? = null
)
