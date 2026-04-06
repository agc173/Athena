package com.agc.bwitch.domain.rituals

import kotlinx.datetime.LocalDate

interface DailyRitualRepository {
    fun getTemplateForDate(date: LocalDate): DailyRitualTemplate
    fun isCompletedOn(date: LocalDate): Boolean
    fun getStreakForDate(date: LocalDate): Int
    fun completeOn(date: LocalDate): Int
}
