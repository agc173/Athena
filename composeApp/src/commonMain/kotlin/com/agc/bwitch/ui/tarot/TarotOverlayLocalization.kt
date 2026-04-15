package com.agc.bwitch.ui.tarot

import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.localization.TarotStrings

private enum class TarotUiLanguage { ES, EN, PT, RU, FR, IT, DE }

data class TarotOverlayLabels(
    val cardName: String,
    val orientation: String,
    val position: String? = null,
)

fun tarotOverlayLabels(
    card: TarotCard,
    strings: TarotStrings,
): TarotOverlayLabels {
    val language = strings.toTarotUiLanguage()
    return TarotOverlayLabels(
        cardName = tarotCardName(card.id, language).uppercase(),
        orientation = tarotOrientationLabel(card.upright, strings),
        position = card.position?.let { tarotPositionLabel(it, strings) },
    )
}

private fun TarotStrings.toTarotUiLanguage(): TarotUiLanguage = when (languageCode.lowercase()) {
    "es" -> TarotUiLanguage.ES
    "pt" -> TarotUiLanguage.PT
    "ru" -> TarotUiLanguage.RU
    "fr" -> TarotUiLanguage.FR
    "it" -> TarotUiLanguage.IT
    "de" -> TarotUiLanguage.DE
    else -> TarotUiLanguage.EN
}

private fun tarotOrientationLabel(upright: Boolean?, strings: TarotStrings): String {
    return if (upright == false) {
        strings.overlayOrientationReversed
    } else if (upright == true) {
        strings.overlayOrientationUpright
    } else {
        strings.overlayOrientationUnknown
    }.uppercase()
}

private fun tarotPositionLabel(position: TarotCardPosition, strings: TarotStrings): String {
    return when (position) {
        TarotCardPosition.PAST -> strings.pastLabel
        TarotCardPosition.PRESENT -> strings.presentLabel
        TarotCardPosition.FUTURE -> strings.futureLabel
    }
        .uppercase()
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

        TarotUiLanguage.PT -> when (slug) {
            "fool" -> "O Louco"
            "magician" -> "O Mago"
            "high_priestess" -> "A Sacerdotisa"
            "empress" -> "A Imperatriz"
            "emperor" -> "O Imperador"
            "hierophant" -> "O Hierofante"
            "lovers" -> "Os Enamorados"
            "chariot" -> "O Carro"
            "strength" -> "A Força"
            "hermit" -> "O Eremita"
            "wheel_of_fortune" -> "A Roda da Fortuna"
            "justice" -> "A Justiça"
            "hanged_man" -> "O Enforcado"
            "death" -> "A Morte"
            "temperance" -> "A Temperança"
            "devil" -> "O Diabo"
            "tower" -> "A Torre"
            "star" -> "A Estrela"
            "moon" -> "A Lua"
            "sun" -> "O Sol"
            "judgement" -> "O Julgamento"
            "world" -> "O Mundo"
            else -> fallbackName(cardId)
        }

        TarotUiLanguage.RU -> when (slug) {
            "fool" -> "Шут"
            "magician" -> "Маг"
            "high_priestess" -> "Верховная Жрица"
            "empress" -> "Императрица"
            "emperor" -> "Император"
            "hierophant" -> "Иерофант"
            "lovers" -> "Влюблённые"
            "chariot" -> "Колесница"
            "strength" -> "Сила"
            "hermit" -> "Отшельник"
            "wheel_of_fortune" -> "Колесо Фортуны"
            "justice" -> "Справедливость"
            "hanged_man" -> "Повешенный"
            "death" -> "Смерть"
            "temperance" -> "Умеренность"
            "devil" -> "Дьявол"
            "tower" -> "Башня"
            "star" -> "Звезда"
            "moon" -> "Луна"
            "sun" -> "Солнце"
            "judgement" -> "Суд"
            "world" -> "Мир"
            else -> fallbackName(cardId)
        }

        TarotUiLanguage.FR -> when (slug) {
            "fool" -> "Le Mat"
            "magician" -> "Le Bateleur"
            "high_priestess" -> "La Papesse"
            "empress" -> "L'Impératrice"
            "emperor" -> "L'Empereur"
            "hierophant" -> "Le Pape"
            "lovers" -> "L'Amoureux"
            "chariot" -> "Le Chariot"
            "strength" -> "La Force"
            "hermit" -> "L'Hermite"
            "wheel_of_fortune" -> "La Roue de Fortune"
            "justice" -> "La Justice"
            "hanged_man" -> "Le Pendu"
            "death" -> "La Mort"
            "temperance" -> "Tempérance"
            "devil" -> "Le Diable"
            "tower" -> "La Maison Dieu"
            "star" -> "L'Étoile"
            "moon" -> "La Lune"
            "sun" -> "Le Soleil"
            "judgement" -> "Le Jugement"
            "world" -> "Le Monde"
            else -> fallbackName(cardId)
        }

        TarotUiLanguage.IT -> when (slug) {
            "fool" -> "Il Matto"
            "magician" -> "Il Mago"
            "high_priestess" -> "La Papessa"
            "empress" -> "L'Imperatrice"
            "emperor" -> "L'Imperatore"
            "hierophant" -> "Il Papa"
            "lovers" -> "Gli Amanti"
            "chariot" -> "Il Carro"
            "strength" -> "La Forza"
            "hermit" -> "L'Eremita"
            "wheel_of_fortune" -> "La Ruota della Fortuna"
            "justice" -> "La Giustizia"
            "hanged_man" -> "L'Appeso"
            "death" -> "La Morte"
            "temperance" -> "La Temperanza"
            "devil" -> "Il Diavolo"
            "tower" -> "La Torre"
            "star" -> "La Stella"
            "moon" -> "La Luna"
            "sun" -> "Il Sole"
            "judgement" -> "Il Giudizio"
            "world" -> "Il Mondo"
            else -> fallbackName(cardId)
        }

        TarotUiLanguage.DE -> when (slug) {
            "fool" -> "Der Narr"
            "magician" -> "Der Magier"
            "high_priestess" -> "Die Hohepriesterin"
            "empress" -> "Die Herrscherin"
            "emperor" -> "Der Herrscher"
            "hierophant" -> "Der Hierophant"
            "lovers" -> "Die Liebenden"
            "chariot" -> "Der Wagen"
            "strength" -> "Die Kraft"
            "hermit" -> "Der Eremit"
            "wheel_of_fortune" -> "Das Rad des Schicksals"
            "justice" -> "Die Gerechtigkeit"
            "hanged_man" -> "Der Gehängte"
            "death" -> "Der Tod"
            "temperance" -> "Die Mäßigkeit"
            "devil" -> "Der Teufel"
            "tower" -> "Der Turm"
            "star" -> "Der Stern"
            "moon" -> "Der Mond"
            "sun" -> "Die Sonne"
            "judgement" -> "Das Gericht"
            "world" -> "Die Welt"
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
        TarotUiLanguage.PT -> when (rank) {
            "ace" -> "Ás"
            "two" -> "Dois"
            "three" -> "Três"
            "four" -> "Quatro"
            "five" -> "Cinco"
            "six" -> "Seis"
            "seven" -> "Sete"
            "eight" -> "Oito"
            "nine" -> "Nove"
            "ten" -> "Dez"
            "page" -> "Pajem"
            "knight" -> "Cavaleiro"
            "queen" -> "Rainha"
            "king" -> "Rei"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.RU -> when (rank) {
            "ace" -> "Туз"
            "two" -> "Двойка"
            "three" -> "Тройка"
            "four" -> "Четвёрка"
            "five" -> "Пятёрка"
            "six" -> "Шестёрка"
            "seven" -> "Семёрка"
            "eight" -> "Восьмёрка"
            "nine" -> "Девятка"
            "ten" -> "Десятка"
            "page" -> "Паж"
            "knight" -> "Рыцарь"
            "queen" -> "Королева"
            "king" -> "Король"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.FR -> when (rank) {
            "ace" -> "As"
            "two" -> "Deux"
            "three" -> "Trois"
            "four" -> "Quatre"
            "five" -> "Cinq"
            "six" -> "Six"
            "seven" -> "Sept"
            "eight" -> "Huit"
            "nine" -> "Neuf"
            "ten" -> "Dix"
            "page" -> "Valet"
            "knight" -> "Chevalier"
            "queen" -> "Reine"
            "king" -> "Roi"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.IT -> when (rank) {
            "ace" -> "Asso"
            "two" -> "Due"
            "three" -> "Tre"
            "four" -> "Quattro"
            "five" -> "Cinque"
            "six" -> "Sei"
            "seven" -> "Sette"
            "eight" -> "Otto"
            "nine" -> "Nove"
            "ten" -> "Dieci"
            "page" -> "Fante"
            "knight" -> "Cavaliere"
            "queen" -> "Regina"
            "king" -> "Re"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.DE -> when (rank) {
            "ace" -> "Ass"
            "two" -> "Zwei"
            "three" -> "Drei"
            "four" -> "Vier"
            "five" -> "Fünf"
            "six" -> "Sechs"
            "seven" -> "Sieben"
            "eight" -> "Acht"
            "nine" -> "Neun"
            "ten" -> "Zehn"
            "page" -> "Page"
            "knight" -> "Ritter"
            "queen" -> "Königin"
            "king" -> "König"
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
        TarotUiLanguage.PT -> when (suit) {
            "wands" -> "Paus"
            "cups" -> "Copas"
            "swords" -> "Espadas"
            "pentacles" -> "Ouros"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.RU -> when (suit) {
            "wands" -> "Жезлов"
            "cups" -> "Кубков"
            "swords" -> "Мечей"
            "pentacles" -> "Пентаклей"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.FR -> when (suit) {
            "wands" -> "Bâtons"
            "cups" -> "Coupes"
            "swords" -> "Épées"
            "pentacles" -> "Deniers"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.IT -> when (suit) {
            "wands" -> "Bastoni"
            "cups" -> "Coppe"
            "swords" -> "Spade"
            "pentacles" -> "Denari"
            else -> return fallbackName(cardId)
        }
        TarotUiLanguage.DE -> when (suit) {
            "wands" -> "Stäbe"
            "cups" -> "Kelche"
            "swords" -> "Schwerter"
            "pentacles" -> "Münzen"
            else -> return fallbackName(cardId)
        }
    }

    return when (language) {
        TarotUiLanguage.ES -> "$rankLabel DE $suitLabel"
        TarotUiLanguage.EN -> "$rankLabel OF $suitLabel"
        TarotUiLanguage.PT -> "$rankLabel DE $suitLabel"
        TarotUiLanguage.RU -> "$rankLabel $suitLabel"
        TarotUiLanguage.FR -> "$rankLabel DE $suitLabel"
        TarotUiLanguage.IT -> "$rankLabel DI $suitLabel"
        TarotUiLanguage.DE -> "$rankLabel DER $suitLabel"
    }
}

private fun fallbackName(cardId: String): String {
    return cardId
        .substringAfter("major-")
        .substringAfter("minor-")
        .split('-')
        .joinToString(" ") { token -> token.replace('_', ' ') }
}
