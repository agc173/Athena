package com.agc.bwitch.domain.astrology.natal

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlin.math.floor

/**
 * User-entered birth date and time in local civil time.
 *
 * Prefer [toUtc] with an IANA time-zone ID so daylight-saving and historical rules are
 * resolved by kotlinx.datetime. The fixed offset is kept only for legacy callers/tests.
 */
@Serializable
data class BirthDateTimeLocal(
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Double = 0.0,
    val timezoneOffsetMinutes: Int? = null,
)

/**
 * Converts local civil birth date/time to UTC using IANA time-zone rules.
 */
fun BirthDateTimeLocal.toUtc(timezoneId: String): BirthDateTimeUtc = toUtc(TimeZone.of(timezoneId))

/**
 * Converts local civil birth date/time to UTC using IANA time-zone rules.
 */
fun BirthDateTimeLocal.toUtc(timeZone: TimeZone): BirthDateTimeUtc {
    val localDateTime = toLocalDateTime()
    val utcDateTime = localDateTime.toInstant(timeZone).toLocalDateTime(TimeZone.UTC)

    return utcDateTime.toBirthDateTimeUtc()
}

/**
 * Converts local civil birth date/time to UTC using the legacy fixed offset.
 */
fun BirthDateTimeLocal.toUtc(): BirthDateTimeUtc {
    val offsetMinutes = timezoneOffsetMinutes ?: error("timezoneOffsetMinutes is required for fixed-offset UTC conversion")
    val utcDateTime = toLocalDateTime().toInstant(
        offset = kotlinx.datetime.UtcOffset(seconds = offsetMinutes * SecondsPerMinute),
    ).toLocalDateTime(TimeZone.UTC)

    return utcDateTime.toBirthDateTimeUtc()
}

private fun BirthDateTimeLocal.toLocalDateTime(): LocalDateTime {
    val wholeSecond = floor(second).toInt()
    val nanosecond = ((second - wholeSecond) * NanosecondsPerSecond).toInt()
    return LocalDateTime(
        year = year,
        monthNumber = month,
        dayOfMonth = day,
        hour = hour,
        minute = minute,
        second = wholeSecond,
        nanosecond = nanosecond,
    )
}

private fun LocalDateTime.toBirthDateTimeUtc(): BirthDateTimeUtc = BirthDateTimeUtc(
    year = year,
    month = monthNumber,
    day = dayOfMonth,
    hour = hour,
    minute = minute,
    second = second + nanosecond.toDouble() / NanosecondsPerSecond,
)

private const val SecondsPerMinute = 60
private const val NanosecondsPerSecond = 1_000_000_000
