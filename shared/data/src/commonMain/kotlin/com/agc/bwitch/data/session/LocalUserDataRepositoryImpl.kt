package com.agc.bwitch.data.session

import com.agc.bwitch.data.astrology.birthchart.SettingsBirthChartRepository
import com.agc.bwitch.data.userprofile.SettingsUserProfileRepository
import com.agc.bwitch.domain.session.LocalUserDataRepository

class LocalUserDataRepositoryImpl(
    private val birthChartLocal: SettingsBirthChartRepository,
    private val userProfileLocal: SettingsUserProfileRepository
) : LocalUserDataRepository {

    override suspend fun clear() {
        // Orden no crítico, pero limpiamos ambos
        runCatching { birthChartLocal.clear() }
        runCatching { userProfileLocal.clear() }
    }
}