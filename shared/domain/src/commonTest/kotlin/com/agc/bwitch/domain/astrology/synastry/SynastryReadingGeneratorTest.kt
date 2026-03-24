package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SynastryReadingGeneratorTest {

    private val resolver = DefaultSynastryCompatibilityResolver()
    private val generator = SynastryReadingGenerator()

    @Test
    fun `with sun only depth is basic`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.aries),
            personB = SynastryPersonInput(sunSign = ZodiacSign.libra),
        )

        val reading = generator(input)

        assertEquals(SynastryReadingDepth.BASIC, reading.structured.depthInfo.depth)
        assertEquals(2, reading.structured.depthInfo.availablePoints)
    }

    @Test
    fun `with partial extra data depth is partial`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(
                sunSign = ZodiacSign.aries,
                moonSign = ZodiacSign.gemini,
            ),
            personB = SynastryPersonInput(sunSign = ZodiacSign.libra),
        )

        val reading = generator(input)

        assertEquals(SynastryReadingDepth.PARTIAL, reading.structured.depthInfo.depth)
    }

    @Test
    fun `with all points depth is complete`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(
                sunSign = ZodiacSign.taurus,
                moonSign = ZodiacSign.cancer,
                risingSign = ZodiacSign.virgo,
            ),
            personB = SynastryPersonInput(
                sunSign = ZodiacSign.cancer,
                moonSign = ZodiacSign.pisces,
                risingSign = ZodiacSign.capricorn,
            ),
        )

        val reading = generator(input)

        assertEquals(SynastryReadingDepth.COMPLETE, reading.structured.depthInfo.depth)
        assertEquals(6, reading.structured.depthInfo.availablePoints)
    }

    @Test
    fun `resolver always returns five dimensions and bounded scores`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.scorpio, moonSign = ZodiacSign.aries),
            personB = SynastryPersonInput(sunSign = ZodiacSign.aquarius, risingSign = ZodiacSign.gemini),
        )

        val structured = resolver.resolve(input)

        assertEquals(SynastryDimension.entries.size, structured.scores.size)
        structured.scores.values.forEach { score ->
            assertTrue(score.value in 0..100)
        }
        assertTrue(structured.overallScore.value in 0..100)
        assertNotNull(structured.archetype)
    }

    @Test
    fun `narrative is generated and non empty`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(displayName = "A", sunSign = ZodiacSign.aries),
            personB = SynastryPersonInput(displayName = "B", sunSign = ZodiacSign.libra),
        )

        val reading = generator(input)

        assertTrue(reading.narrative.isNotBlank())
        assertTrue(reading.narrative.contains("A"))
        assertTrue(reading.narrative.contains("B"))
    }

    @Test
    fun `aries libra solar only returns valid reading`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.aries),
            personB = SynastryPersonInput(sunSign = ZodiacSign.libra),
        )

        val reading = generator(input)

        assertEquals(SynastryReadingDepth.BASIC, reading.structured.depthInfo.depth)
        assertFalse(reading.structured.scores.isEmpty())
        assertTrue(reading.structured.strengths.isNotEmpty() || reading.structured.tensions.isNotEmpty())
    }

    @Test
    fun `taurus cancer richer data returns valid reading`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(
                sunSign = ZodiacSign.taurus,
                moonSign = ZodiacSign.cancer,
                risingSign = ZodiacSign.capricorn,
            ),
            personB = SynastryPersonInput(
                sunSign = ZodiacSign.cancer,
                moonSign = ZodiacSign.taurus,
                risingSign = ZodiacSign.virgo,
            ),
        )

        val reading = generator(input)

        assertTrue(reading.structured.overallScore.value in 0..100)
        assertNotNull(reading.structured.archetype)
        assertTrue(reading.narrative.isNotBlank())
    }
}
