package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.datetime.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class PrefetchDailyHoroscopeUseCase(
    private val syncController: HoroscopeDailySyncController,
    private val clock: Clock,
) {

    suspend operator fun invoke(daysAhead: Int) = coroutineScope {

        val today = clock.todayIso()

        (0..daysAhead).map { offset ->
            async {
                val dateIso = today.plusDays(offset)

                syncController.pull(dateIso)
            }
        }.awaitAll()
    }
}