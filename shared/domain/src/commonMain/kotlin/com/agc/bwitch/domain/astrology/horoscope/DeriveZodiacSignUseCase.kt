package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.datetime.LocalDate

class DeriveZodiacSignUseCase {
    operator fun invoke(birthDate: LocalDate): ZodiacSign =
        ZodiacSignResolver.fromBirthDate(birthDate)
}
