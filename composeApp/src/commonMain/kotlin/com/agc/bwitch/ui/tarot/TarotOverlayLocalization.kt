package com.agc.bwitch.ui.tarot

import androidx.compose.ui.text.intl.Locale
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotCardPosition

enum class TarotUiLanguage {
    ES,
    EN,
}

data class TarotOverlayLabels(
    val cardName: String,
    val orientation: String,
    val position: String? = null,
)

fun currentTarotUiLanguage(): TarotUiLanguage {
    return when (Locale.current.language.lowercase()) {
        "es" -> TarotUiLanguage.ES
        else -> TarotUiLanguage.EN
    }
}

fun tarotOverlayLabels(
    card: TarotCard,
    language: TarotUiLanguage,
): TarotOverlayLabels {
    return TarotOverlayLabels(
        cardName = tarotCardName(card.id, language).uppercase(),
        orientation = tarotOrientationLabel(card.upright, language),
        position = card.position?.let { tarotPositionLabel(it, language) },
    )
}

private fun tarotOrientationLabel(upright: Boolean?, language: TarotUiLanguage): String {
    return when (language) {
        TarotUiLanguage.ES -> if (upright == false) "INVERTIDA" else if (upright == true) "AL DERECHO" else "DESCONOCIDA"
        TarotUiLanguage.EN -> if (upright == false) "REVERSED" else if (upright == true) "UPRIGHT" else "UNKNOWN"
    }
}

private fun tarotPositionLabel(position: TarotCardPosition, language: TarotUiLanguage): String {
    return when (language) {
        TarotUiLanguage.ES -> when (position) {
            TarotCardPosition.PAST -> "PASADO"
            TarotCardPosition.PRESENT -> "PRESENTE"
            TarotCardPosition.FUTURE -> "FUTURO"
        }

        TarotUiLanguage.EN -> when (position) {
            TarotCardPosition.PAST -> "PAST"
            TarotCardPosition.PRESENT -> "PRESENT"
            TarotCardPosition.FUTURE -> "FUTURE"
        }
    }
}

private fun tarotCardName(cardId: String, language: TarotUiLanguage): String {
    return when {
        cardId.startsWith("major-") -> majorArcanaName(cardId, language)
        cardId.startsWith("minor-") -> minorArcanaName(cardId, language)
        else -> fallbackName(cardId)
    }
}

private fun majorArcanaName(cardId: String, language: TarotUiLanguage): String {
    val slug = cardId
        .split('-')
        .drop(2)
        .joinToString("_")
    return when (language) {
        TarotUiLanguage.ES -> when (slug) {
            "fool" -> "El Loco"
            "magician" -> "El Mago"
            "high_priestess" -> "La Sacerdotisa"
            "empress" -> "La Emperatriz"
            "emperor" -> "El Emperador"
            "hierophant" -> "El Hierofante"
            "lovers" -> "Los Enamorados"
            "chariot" -> "El Carro"
            "strength" -> "La Fuerza"
            "hermit" -> "El Ermitaño"
            "wheel_of_fortune" -> "La Rueda de la Fortuna"
            "justice" -> "La Justicia"
            "hanged_man" -> "El Colgado"
            "death" -> "La Muerte"
            "temperance" -> "La Templanza"
            "devil" -> "El Diablo"
            "tower" -> "La Torre"
            "star" -> "La Estrella"
            "moon" -> "La Luna"
            "sun" -> "El Sol"
            "judgement" -> "El Juicio"
            "world" -> "El Mundo"
            else -> fallbackName(cardId)
        }

        TarotUiLanguage.EN -> when (slug) {
            "fool" -> "The Fool"
            "magician" -> "The Magician"
            "high_priestess" -> "The High Priestess"
            "empress" -> "The Empress"
            "emperor" -> "The Emperor"
            "hierophant" -> "The Hierophant"
            "lovers" -> "The Lovers"
            "chariot" -> "The Chariot"
            "strength" -> "Strength"
            "hermit" -> "The Hermit"
            "wheel_of_fortune" -> "Wheel of Fortune"
            "justice" -> "Justice"
            "hanged_man" -> "The Hanged Man"
            "death" -> "Death"
            "temperance" -> "Temperance"
            "devil" -> "The Devil"
            "tower" -> "The Tower"
            "star" -> "The Star"
            "moon" -> "The Moon"
            "sun" -> "The Sun"
            "judgement" -> "Judgement"
            "world" -> "The World"
            else -> fallbackName(cardId)
        }
    }
}

private fun minorArcanaName(cardId: String, language: TarotUiLanguage): String {
    val parts = cardId.split("-")
    if (parts.size < 3) return fallbackName(cardId)

    val rank = parts[1]
    val suit = parts[2]

    val rankLabel = when (language) {
        TarotUiLanguage.ES -> when (rank) {
            "ace" -> "As"
            "two" -> "Dos"
            "three" -> "Tres"
            "four" -> "Cuatro"
            "five" -> "Cinco"
            "six" -> "Seis"
            "seven" -> "Siete"
            "eight" -> "Ocho"
            "nine" -> "Nueve"
            "ten" -> "Diez"
            "page" -> "Paje"
            "knight" -> "Caballero"
            "queen" -> "Reina"
            "king" -> "Rey"
            else -> return fallbackName(cardId)
        }

        TarotUiLanguage.EN -> when (rank) {
            "ace" -> "Ace"
            "two" -> "Two"
            "three" -> "Three"
            "four" -> "Four"
            "five" -> "Five"
            "six" -> "Six"
            "seven" -> "Seven"
            "eight" -> "Eight"
            "nine" -> "Nine"
            "ten" -> "Ten"
            "page" -> "Page"
            "knight" -> "Knight"
            "queen" -> "Queen"
            "king" -> "King"
            else -> return fallbackName(cardId)
        }
    }

    val suitLabel = when (language) {
        TarotUiLanguage.ES -> when (suit) {
            "wands" -> "Bastos"
            "cups" -> "Copas"
            "swords" -> "Espadas"
            "pentacles" -> "Oros"
            else -> return fallbackName(cardId)
        }

        TarotUiLanguage.EN -> when (suit) {
            "wands" -> "Wands"
            "cups" -> "Cups"
            "swords" -> "Swords"
            "pentacles" -> "Pentacles"
            else -> return fallbackName(cardId)
        }
    }

    return when (language) {
        TarotUiLanguage.ES -> "$rankLabel DE $suitLabel"
        TarotUiLanguage.EN -> "$rankLabel OF $suitLabel"
    }
}

private fun fallbackName(cardId: String): String {
    return cardId
        .substringAfter("major-")
        .substringAfter("minor-")
        .split('-')
        .joinToString(" ") { token -> token.replace('_', ' ') }
}
