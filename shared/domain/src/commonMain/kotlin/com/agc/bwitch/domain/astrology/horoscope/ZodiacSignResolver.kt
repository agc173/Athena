package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.datetime.LocalDate

object ZodiacSignResolver {

    fun fromBirthDate(birthDate: LocalDate): ZodiacSign {
        val month = birthDate.monthNumber
        val day = birthDate.dayOfMonth

        return when {
            (month == 3 && day >= 21) || (month == 4 && day <= 19) -> ZodiacSign.aries
            (month == 4 && day >= 20) || (month == 5 && day <= 20) -> ZodiacSign.taurus
            (month == 5 && day >= 21) || (month == 6 && day <= 20) -> ZodiacSign.gemini
            (month == 6 && day >= 21) || (month == 7 && day <= 22) -> ZodiacSign.cancer
            (month == 7 && day >= 23) || (month == 8 && day <= 22) -> ZodiacSign.leo
            (month == 8 && day >= 23) || (month == 9 && day <= 22) -> ZodiacSign.virgo
            (month == 9 && day >= 23) || (month == 10 && day <= 22) -> ZodiacSign.libra
            (month == 10 && day >= 23) || (month == 11 && day <= 21) -> ZodiacSign.scorpio
            (month == 11 && day >= 22) || (month == 12 && day <= 21) -> ZodiacSign.sagittarius
            (month == 12 && day >= 22) || (month == 1 && day <= 19) -> ZodiacSign.capricorn
            (month == 1 && day >= 20) || (month == 2 && day <= 18) -> ZodiacSign.aquarius
            else -> ZodiacSign.pisces
        }
    }
}
