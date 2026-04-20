package com.agc.bwitch.domain.astrology.horoscope

class PullDailyHoroscopeUseCase(
    private val syncController: HoroscopeDailySyncController,
) {
    suspend operator fun invoke(dateIso: String, languageCode: String) {
        syncController.pull(dateIso, languageCode)
    }
}
