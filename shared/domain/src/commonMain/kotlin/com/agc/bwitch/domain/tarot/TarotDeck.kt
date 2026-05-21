package com.agc.bwitch.domain.tarot

enum class TarotDeckId(val value: String) {
    RIDER_WAITE("rider_waite"),
    ARCANA_NOCTIS("arcana_noctis"),
    ;

    companion object {
        fun fromValue(rawValue: String?): TarotDeckId? =
            entries.firstOrNull { it.value == rawValue }
    }
}

data class TarotDeckDefinition(
    val id: TarotDeckId,
    val displayNameKey: String,
    val previewCardId: String,
    val isDefault: Boolean,
)

enum class TarotDeckAvailability {
    AVAILABLE,
    LOCKED,
}
