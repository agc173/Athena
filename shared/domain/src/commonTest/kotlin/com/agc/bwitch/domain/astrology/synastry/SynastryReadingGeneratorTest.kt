package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class SynastryReadingGeneratorTest {

    private val resolver = DefaultSynastryCompatibilityResolver()
    private val generator = SynastryReadingGenerator()
    private val overlayGenerator = SynastryDailyOverlayGenerator()

    @Test
    fun `deterministic for same input and date`() {
        val input = sampleInput()
        val date = LocalDate.parse("2026-03-24")

        val first = resolver.resolve(input, date)
        val second = resolver.resolve(input, date)

        assertEquals(first.scores, second.scores)
        assertEquals(first.overallScore, second.overallScore)
        assertEquals(first.baseProfile, second.baseProfile)
    }

    @Test
    fun `varies across different dates`() {
        val input = sampleInput()

        val today = resolver.resolve(input, LocalDate.parse("2026-03-24"))
        val nextWeek = resolver.resolve(input, LocalDate.parse("2026-03-31"))

        assertNotEquals(today.scores, nextWeek.scores)
    }

    @Test
    fun `continuity between consecutive days stays reasonable`() {
        val input = sampleInput()
        val day1 = resolver.resolve(input, LocalDate.parse("2026-03-24"))
        val day2 = resolver.resolve(input, LocalDate.parse("2026-03-25"))

        SynastryDimension.entries.forEach { dimension ->
            val delta = abs(day1.scores.getValue(dimension).toFiveStarRating() - day2.scores.getValue(dimension).toFiveStarRating())
            assertTrue(delta <= 1.0, "delta too high on $dimension: $delta")
        }
    }

    @Test
    fun `always returns the 4 visible metrics in range`() {
        val structured = resolver.resolve(sampleInput(), LocalDate.parse("2026-03-24"))

        assertEquals(4, structured.scores.size)
        assertEquals(SynastryDimension.entries.toSet(), structured.scores.keys)
        structured.scores.values.forEach { assertTrue(it.value in 0..100) }
        assertTrue(structured.strengths.isNotEmpty())
        assertTrue(structured.tensions.isNotEmpty())
        assertTrue(structured.guidance.isNotEmpty())
    }

    @Test
    fun `star conversion supports half stars`() {
        assertEquals(2.5, SynastryScore.from(49).toFiveStarRating())
        assertEquals(3.0, SynastryScore.from(61).toFiveStarRating())
        assertEquals(4.5, SynastryScore.from(89).toFiveStarRating())
    }

    @Test
    fun `total stars are in reasonable band for representative cases`() {
        val inputs = listOf(
            SynastryInput(SynastryPersonInput(ZodiacSign.aries), SynastryPersonInput(ZodiacSign.libra)),
            SynastryInput(SynastryPersonInput(ZodiacSign.taurus), SynastryPersonInput(ZodiacSign.scorpio)),
            SynastryInput(SynastryPersonInput(ZodiacSign.gemini), SynastryPersonInput(ZodiacSign.virgo)),
            SynastryInput(SynastryPersonInput(ZodiacSign.cancer), SynastryPersonInput(ZodiacSign.capricorn)),
            SynastryInput(SynastryPersonInput(ZodiacSign.leo), SynastryPersonInput(ZodiacSign.aquarius)),
        )

        val totals = inputs.map { input ->
            resolver.resolve(input, LocalDate.parse("2026-03-24")).scores.values.sumOf { it.toFiveStarRating() }
        }

        assertTrue(totals.all { it in 8.0..18.5 }, "Totals out of range: $totals")
        assertTrue(totals.any { it in 11.0..16.0 })
    }

    @Test
    fun `daily overlay is valid with distinct highlighted and sensitive`() {
        val input = sampleInput()
        val date = LocalDate.parse("2026-03-24")
        val structured = resolver.resolve(input, date)

        val overlay = overlayGenerator.generate(input, structured, date)

        assertNotEquals(overlay.highlightedDimension, overlay.sensitiveDimension)
        assertEquals(3, overlay.axes.size)
        assertEquals(SynastryEnergyAxis.entries.toSet(), overlay.axes.map { it.axis }.toSet())
        assertTrue(overlay.axes.all { it.value in -100..100 })
    }

    @Test
    fun `overlay has autonomy and is not always top and bottom score`() {
        val input = sampleInput()
        val dateWindow = listOf(
            LocalDate.parse("2026-03-24"),
            LocalDate.parse("2026-03-25"),
            LocalDate.parse("2026-03-26"),
            LocalDate.parse("2026-03-27"),
            LocalDate.parse("2026-03-28"),
            LocalDate.parse("2026-03-29"),
            LocalDate.parse("2026-03-30"),
            LocalDate.parse("2026-03-31"),
            LocalDate.parse("2026-04-01"),
            LocalDate.parse("2026-04-02"),
            LocalDate.parse("2026-04-03"),
            LocalDate.parse("2026-04-04"),
            LocalDate.parse("2026-04-05"),
            LocalDate.parse("2026-04-06"),
            LocalDate.parse("2026-04-07"),
            LocalDate.parse("2026-04-08"),
            LocalDate.parse("2026-04-09"),
            LocalDate.parse("2026-04-10"),
            LocalDate.parse("2026-04-11"),
            LocalDate.parse("2026-04-12"),
            LocalDate.parse("2026-04-13"),
            LocalDate.parse("2026-04-14"),
            LocalDate.parse("2026-04-15"),
            LocalDate.parse("2026-04-16"),
            LocalDate.parse("2026-04-17"),
            LocalDate.parse("2026-04-18"),
            LocalDate.parse("2026-04-19"),
            LocalDate.parse("2026-04-20"),
            LocalDate.parse("2026-04-21"),
            LocalDate.parse("2026-04-22"),
        )

        val mismatches = dateWindow.count { date ->
            val structured = resolver.resolve(input, date)
            val overlay = overlayGenerator.generate(input, structured, date)
            val topDimension = structured.scores.maxByOrNull { it.value.value }?.key
            val bottomDimension = structured.scores.minByOrNull { it.value.value }?.key
            overlay.highlightedDimension != topDimension || overlay.sensitiveDimension != bottomDimension
        }

        assertTrue(mismatches >= 1, "Overlay looks fully tied to score ranking across an extended window")
    }

    @Test
    fun `symmetry on swapped people for same date`() {
        val inputAB = sampleInput()
        val inputBA = SynastryInput(personA = inputAB.personB, personB = inputAB.personA)
        val date = LocalDate.parse("2026-03-24")

        val ab = resolver.resolve(inputAB, date)
        val ba = resolver.resolve(inputBA, date)

        assertEquals(ab.scores, ba.scores)
        assertEquals(ab.overallScore, ba.overallScore)
        assertEquals(ab.baseProfile.familyKey, ba.baseProfile.familyKey)
    }


    @Test
    fun `overlay keeps symmetry when swapping people`() {
        val inputAB = sampleInput()
        val inputBA = SynastryInput(personA = inputAB.personB, personB = inputAB.personA)
        val date = LocalDate.parse("2026-03-27")
        val abStructured = resolver.resolve(inputAB, date)
        val baStructured = resolver.resolve(inputBA, date)

        val overlayAB = overlayGenerator.generate(inputAB, abStructured, date)
        val overlayBA = overlayGenerator.generate(inputBA, baStructured, date)

        assertEquals(overlayAB.highlightedDimension, overlayBA.highlightedDimension)
        assertEquals(overlayAB.sensitiveDimension, overlayBA.sensitiveDimension)
        assertEquals(overlayAB.axes, overlayBA.axes)
    }

    @Test
    fun `different pairs in same family get refined base profiles`() {
        val date = LocalDate.parse("2026-03-24")
        val ariesScorpio = resolver.resolve(
            SynastryInput(SynastryPersonInput(ZodiacSign.aries), SynastryPersonInput(ZodiacSign.scorpio)),
            date,
        )
        val leoCancer = resolver.resolve(
            SynastryInput(SynastryPersonInput(ZodiacSign.leo), SynastryPersonInput(ZodiacSign.cancer)),
            date,
        )
        val sagPisces = resolver.resolve(
            SynastryInput(SynastryPersonInput(ZodiacSign.sagittarius), SynastryPersonInput(ZodiacSign.pisces)),
            date,
        )

        assertNotEquals(ariesScorpio.baseProfile.metrics, leoCancer.baseProfile.metrics)
        assertNotEquals(leoCancer.baseProfile.metrics, sagPisces.baseProfile.metrics)
        assertNotEquals(ariesScorpio.baseProfile.familyKey, leoCancer.baseProfile.familyKey)
    }

    @Test
    fun `generator keeps depth logic and daily overlay`() {
        val input = SynastryInput(
            personA = SynastryPersonInput(ZodiacSign.aries),
            personB = SynastryPersonInput(ZodiacSign.libra),
        )

        val reading = generator(input, LocalDate.parse("2026-03-24"))

        assertEquals(SynastryReadingDepth.BASIC, reading.structured.depthInfo.depth)
        assertFalse(reading.narrative.isBlank())
        assertTrue(reading.dailyOverlay != null)
    }

    private fun sampleInput(): SynastryInput = SynastryInput(
        personA = SynastryPersonInput(
            sunSign = ZodiacSign.aries,
            moonSign = ZodiacSign.scorpio,
            risingSign = ZodiacSign.sagittarius,
        ),
        personB = SynastryPersonInput(
            sunSign = ZodiacSign.libra,
            moonSign = ZodiacSign.gemini,
            risingSign = ZodiacSign.capricorn,
        ),
    )
}
