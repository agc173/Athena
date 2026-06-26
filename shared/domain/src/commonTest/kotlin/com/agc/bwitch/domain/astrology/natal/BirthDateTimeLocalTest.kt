package com.agc.bwitch.domain.astrology.natal

import kotlin.test.Test
import kotlin.test.assertEquals

class BirthDateTimeLocalTest {
    @Test
    fun convertsMadridSummerOffsetToUtc() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 12,
            minute = 30,
            timezoneOffsetMinutes = 120,
        ).toUtc()

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
    fun convertsMadridWinterOffsetToUtc() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 1,
            day = 15,
            hour = 12,
            minute = 30,
            timezoneOffsetMinutes = 60,
        ).toUtc()

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
    fun rollsDateBackwardWhenPositiveOffsetCrossesUtcMidnight() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 0,
            minute = 30,
            timezoneOffsetMinutes = 120,
        ).toUtc()

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
    fun rollsDateForwardWhenNegativeOffsetCrossesUtcMidnight() {
        val utc = BirthDateTimeLocal(
            year = 1990,
            month = 7,
            day = 15,
            hour = 23,
            minute = 30,
            timezoneOffsetMinutes = -180,
        ).toUtc()

        assertEquals(
            BirthDateTimeUtc(
                year = 1990,
                month = 7,
                day = 16,
                hour = 2,
                minute = 30,
            ),
            utc,
        )
    }
}
