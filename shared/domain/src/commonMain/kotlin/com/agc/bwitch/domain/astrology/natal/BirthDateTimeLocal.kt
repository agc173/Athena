package com.agc.bwitch.domain.astrology.natal

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.floor

/**
 * User-entered birth date and time in local civil time, plus the explicit UTC offset
 * selected by the caller. This v1 model intentionally does not perform city lookup or
 * time-zone database resolution.
 */
@Serializable
data class BirthDateTimeLocal(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Double = 0.0,
    val timezoneOffsetMinutes: Int,
)

/**
 * Converts local civil birth date/time to UTC using the provided fixed offset.
 */
fun BirthDateTimeLocal.toUtc(): BirthDateTimeUtc {
    val wholeSecond = floor(second).toInt()
    val nanosecond = ((second - wholeSecond) * NanosecondsPerSecond).toInt()
    val utcDateTime = LocalDateTime(
        year = year,
        monthNumber = month,
        dayOfMonth = day,
        hour = hour,
        minute = minute,
        second = wholeSecond,
        nanosecond = nanosecond,
    ).toInstant(
        offset = UtcOffset(seconds = timezoneOffsetMinutes * SecondsPerMinute),
    ).toLocalDateTime(TimeZone.UTC)

    return BirthDateTimeUtc(
        year = utcDateTime.year,
        month = utcDateTime.monthNumber,
        day = utcDateTime.dayOfMonth,
        hour = utcDateTime.hour,
        minute = utcDateTime.minute,
        second = utcDateTime.second + utcDateTime.nanosecond.toDouble() / NanosecondsPerSecond,
    )
}

private const val SecondsPerMinute = 60
private const val NanosecondsPerSecond = 1_000_000_000
