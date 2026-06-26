package com.agc.bwitch.domain.astrology.natal

import kotlin.test.Test
import kotlin.test.assertEquals

class ZodiacLongitudeTest {
    @Test
    fun mapsBoundaryLongitudesToExpectedSigns() {
        assertEquals(ZodiacSign.aries, longitudeToZodiacSign(0.0))
        assertEquals(ZodiacSign.aries, longitudeToZodiacSign(29.999))
        assertEquals(ZodiacSign.taurus, longitudeToZodiacSign(30.0))
        assertEquals(ZodiacSign.pisces, longitudeToZodiacSign(359.999))
    }

    @Test
    fun normalizesNegativeLongitudes() {
        assertEquals(ZodiacSign.pisces, longitudeToZodiacSign(-0.001))
    }

    @Test
    fun normalizesLongitudesGreaterThanFullCircle() {
        assertEquals(ZodiacSign.taurus, longitudeToZodiacSign(390.0))
    }
}
