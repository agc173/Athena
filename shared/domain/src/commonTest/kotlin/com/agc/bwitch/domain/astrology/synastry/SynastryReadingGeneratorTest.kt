package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class SynastryReadingGeneratorTest {

    private val resolver = DefaultSynastryCompatibilityResolver()
    private val generator = SynastryReadingGenerator()
    private val overlayGenerator = SynastryDailyOverlayGenerator()

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
    fun `narrative uses zodiac anchors without person labels`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.aries),
            personB = SynastryPersonInput(sunSign = ZodiacSign.leo),
        )

        val reading = generator(input)

        assertTrue(reading.narrative.isNotBlank())
        assertTrue(reading.narrative.contains("Aries y Leo"))
        assertFalse(reading.narrative.contains("Persona A"))
        assertFalse(reading.narrative.contains("Persona B"))
        assertNotNull(reading.dailyOverlay)
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

    @Test
    fun `solar only scoring is symmetric when swapping people`() {
        val inputAB = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.aries),
            personB = SynastryPersonInput(sunSign = ZodiacSign.libra),
        )
        val inputBA = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.libra),
            personB = SynastryPersonInput(sunSign = ZodiacSign.aries),
        )

        val ab = resolver.resolve(inputAB)
        val ba = resolver.resolve(inputBA)

        assertEquals(ab.overallScore.value, ba.overallScore.value)
        assertEquals(ab.scores, ba.scores)
        assertEquals(ab.strengths, ba.strengths)
        assertEquals(ab.tensions, ba.tensions)
        assertEquals(ab.tags, ba.tags)
    }

    @Test
    fun `partial and complete data scoring is symmetric when swapping people`() {
        val inputAB = SynastryInput(
            personA = SynastryPersonInput(
                sunSign = ZodiacSign.scorpio,
                moonSign = ZodiacSign.pisces,
                risingSign = ZodiacSign.aries,
            ),
            personB = SynastryPersonInput(
                sunSign = ZodiacSign.aquarius,
                moonSign = ZodiacSign.gemini,
                risingSign = ZodiacSign.leo,
            ),
        )
        val inputBA = SynastryInput(
            personA = inputAB.personB,
            personB = inputAB.personA,
        )

        val ab = resolver.resolve(inputAB)
        val ba = resolver.resolve(inputBA)

        assertEquals(ab.overallScore.value, ba.overallScore.value)
        assertEquals(ab.scores, ba.scores)
        assertEquals(ab.strengths, ba.strengths)
        assertEquals(ab.tensions, ba.tensions)
    }

    @Test
    fun `representative case has meaningful score dispersion`() {
        val structured = resolver.resolve(
            SynastryInput(
                personA = SynastryPersonInput(
                    sunSign = ZodiacSign.aries,
                    moonSign = ZodiacSign.cancer,
                    risingSign = ZodiacSign.sagittarius,
                ),
                personB = SynastryPersonInput(
                    sunSign = ZodiacSign.libra,
                    moonSign = ZodiacSign.aquarius,
                    risingSign = ZodiacSign.capricorn,
                ),
            )
        )

        val values = structured.scores.values.map { it.value }
        val spread = values.max() - values.min()

        assertTrue(spread >= 12, "Expected score dispersion >= 12, but was $spread with $values")
        assertTrue(values.any { abs(it - values.average()) >= 6 })
    }

    @Test
    fun `base result is stable for same inputs`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.aries, moonSign = ZodiacSign.scorpio),
            personB = SynastryPersonInput(sunSign = ZodiacSign.libra, risingSign = ZodiacSign.gemini),
        )

        val first = resolver.resolve(input)
        val second = resolver.resolve(input)

        assertEquals(first.scores, second.scores)
        assertEquals(first.overallScore.value, second.overallScore.value)
        assertEquals(first.strengths, second.strengths)
        assertEquals(first.tensions, second.tensions)
    }

    @Test
    fun `daily overlay is deterministic for same date and input`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.taurus),
            personB = SynastryPersonInput(sunSign = ZodiacSign.scorpio),
        )
        val structured = resolver.resolve(input)
        val date = LocalDate.parse("2026-03-24")

        val first = overlayGenerator.generate(input, structured, date)
        val second = overlayGenerator.generate(input, structured, date)

        assertEquals(first, second)
        assertNotEquals(first.highlightedDimension, first.sensitiveDimension)
    }

    @Test
    fun `daily overlay can vary with date`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(sunSign = ZodiacSign.cancer),
            personB = SynastryPersonInput(sunSign = ZodiacSign.capricorn),
        )
        val structured = resolver.resolve(input)

        val first = overlayGenerator.generate(input, structured, LocalDate.parse("2026-03-24"))
        val second = overlayGenerator.generate(input, structured, LocalDate.parse("2026-03-25"))

        val changed = first.dailyEnergyLabel != second.dailyEnergyLabel ||
            first.highlightedDimension != second.highlightedDimension ||
            first.sensitiveDimension != second.sensitiveDimension ||
            first.dailyGuidance != second.dailyGuidance ||
            first.dailyNarrativeFragment != second.dailyNarrativeFragment

        assertTrue(changed, "Expected daily overlay to vary across dates")
        assertNotEquals(second.highlightedDimension, second.sensitiveDimension)
    }
}
