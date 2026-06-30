package com.agc.bwitch.data.economy

import com.agc.bwitch.data.remote.economy.EconomyModulePreviewDto
import com.agc.bwitch.domain.economy.EconomyNextSource
import kotlin.test.Test
import kotlin.test.assertEquals

class EconomyModulePreviewMapperTest {
    @Test
    fun mapsBasicNatalMoonPreviewToDomain() {
        val domain = EconomyModulePreviewDto(
            module = "BASIC_NATAL_CHART",
            nextSource = "MOON",
            cost = 1,
            balance = 1,
            canExecute = true,
        ).toEconomyModulePreview()

        assertEquals("BASIC_NATAL_CHART", domain.module)
        assertEquals(EconomyNextSource.MOON, domain.nextSource)
        assertEquals(1, domain.cost)
    }
}
