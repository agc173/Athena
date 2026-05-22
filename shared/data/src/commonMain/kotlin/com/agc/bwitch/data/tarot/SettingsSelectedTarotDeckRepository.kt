package com.agc.bwitch.data.tarot

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.tarot.SelectedTarotDeckRepository
import com.agc.bwitch.domain.tarot.TarotDeckId
import com.russhwolf.settings.Settings

class SettingsSelectedTarotDeckRepository(
    settingsFactory: SettingsFactory,
) : SelectedTarotDeckRepository {
    private val settings: Settings = settingsFactory.create("bwitch_tarot")

    override fun getSelectedDeckId(): TarotDeckId =
        TarotDeckId.fromValue(settings.getStringOrNull(KEY_SELECTED_DECK_ID)) ?: TarotDeckId.RIDER_WAITE

    override fun setSelectedDeckId(deckId: TarotDeckId) {
        settings.putString(KEY_SELECTED_DECK_ID, deckId.value)
    }

    private companion object {
        const val KEY_SELECTED_DECK_ID = "selected_deck_id"
    }
}
