package com.agc.bwitch.domain.tarot

object TarotDeckCatalog {
    val allDecks: List<TarotDeckDefinition> = listOf(
        TarotDeckDefinition(
            id = TarotDeckId.RIDER_WAITE,
            displayNameKey = "tarot_deck_rider_waite",
            previewCardId = "major-00-fool",
            progressTrackId = "rider_waite",
            isDefault = true,
            requiredCardsToPlay = 0,
        ),
        TarotDeckDefinition(
            id = TarotDeckId.ARCANA_NOCTIS,
            displayNameKey = "tarot_deck_arcana_noctis",
            previewCardId = "major-00-fool",
            progressTrackId = "arcana_noctis",
            isDefault = false,
            requiredCardsToPlay = 78,
        ),
    )

    val defaultDeck: TarotDeckDefinition = allDecks.first { it.isDefault }

    fun getById(id: TarotDeckId): TarotDeckDefinition? =
        allDecks.firstOrNull { it.id == id }

    fun resolvePlayableDeckId(
        selectedDeckId: TarotDeckId,
        progressByTrackId: Map<String, TarotDeckCollectionProgress>,
    ): TarotDeckId {
        val selectedDefinition = getById(selectedDeckId) ?: return defaultDeck.id
        if (selectedDefinition.isDefault || selectedDefinition.requiredCardsToPlay <= 0) return selectedDefinition.id

        val unlockedCount = progressByTrackId[selectedDefinition.progressTrackId]?.unlockedCards?.size ?: 0
        return if (unlockedCount >= selectedDefinition.requiredCardsToPlay) selectedDefinition.id else defaultDeck.id
    }
}
