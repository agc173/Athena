package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.datetime.*

fun Clock.todayIso(): String {
    val today = now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    return today.toString()
}

fun String.plusDays(days: Int): String {
    val date = LocalDate.parse(this)
    return date.plus(days, DateTimeUnit.DAY)
        .toString()
}