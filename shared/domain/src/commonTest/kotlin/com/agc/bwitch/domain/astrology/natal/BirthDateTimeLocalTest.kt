package com.agc.bwitch.domain.astrology.natal

import kotlin.test.Test
import kotlin.test.assertEquals

class BirthDateTimeLocalTest {
    @Test
    fun convertsMadridWinterTimezoneToUtc() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 1,
            day = 15,
            hour = 12,
            minute = 30,
        ).toUtc("Europe/Madrid")

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 1,
                day = 15,
                hour = 11,
                minute = 30,
            ),
            utc,
        )
    }

    @Test
    fun convertsMadridSummerTimezoneToUtc() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 12,
            minute = 30,
        ).toUtc("Europe/Madrid")

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 7,
                day = 15,
                hour = 10,
                minute = 30,
            ),
            utc,
        )
    }

    @Test
    fun convertsNewYorkSummerTimezoneToUtc() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 12,
            minute = 30,
        ).toUtc("America/New_York")

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 7,
                day = 15,
                hour = 16,
                minute = 30,
            ),
            utc,
        )
    }

    @Test
    fun convertsNewYorkWinterTimezoneToUtc() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 1,
            day = 15,
            hour = 12,
            minute = 30,
        ).toUtc("America/New_York")

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 1,
                day = 15,
                hour = 17,
                minute = 30,
            ),
            utc,
        )
    }

    @Test
    fun rollsDateBackwardWhenTimezoneCrossesUtcMidnight() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 0,
            minute = 30,
        ).toUtc("Europe/Madrid")

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 7,
                day = 14,
                hour = 22,
                minute = 30,
            ),
            utc,
        )
    }

    @Test
    fun rollsDateForwardWhenTimezoneCrossesUtcMidnight() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 23,
            minute = 30,
        ).toUtc("America/New_York")

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 7,
                day = 16,
                hour = 3,
                minute = 30,
            ),
            utc,
        )
    }
}
