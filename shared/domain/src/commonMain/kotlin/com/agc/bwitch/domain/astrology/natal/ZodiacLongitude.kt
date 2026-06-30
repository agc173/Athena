package com.agc.bwitch.domain.astrology.natal

import kotlin.math.floor

fun longitudeToZodiacSign(longitudeDegrees: Double): ZodiacSign {
    val normalizedLongitude = longitudeDegrees - (floor(longitudeDegrees / FULL_CIRCLE_DEGREES) * FULL_CIRCLE_DEGREES)
    val signIndex = floor(normalizedLongitude / SIGN_SPAN_DEGREES).toInt()
    return ZodiacSign.entries[signIndex]
}

private const val FULL_CIRCLE_DEGREES = 360.0
private const val SIGN_SPAN_DEGREES = 30.0
