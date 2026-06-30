package com.agc.bwitch.ui.astrology.birthplace

internal fun normalizeBirthplaceSearchText(text: String): String = buildString(text.length) {
    var previousWasSpace = true
    text.trim().lowercase().forEach { char ->
        val normalizedChar = when (char) {
            'á', 'à', 'ä', 'â', 'ã', 'å', 'ā', 'ă', 'ą' -> "a"
            'é', 'è', 'ë', 'ê', 'ē', 'ĕ', 'ė', 'ę', 'ě' -> "e"
            'í', 'ì', 'ï', 'î', 'ī', 'ĭ', 'į', 'ı' -> "i"
            'ó', 'ò', 'ö', 'ô', 'õ', 'ø', 'ō', 'ŏ', 'ő' -> "o"
            'ú', 'ù', 'ü', 'û', 'ū', 'ŭ', 'ů', 'ű', 'ų' -> "u"
            'ý', 'ÿ' -> "y"
            'ñ', 'ń', 'ņ', 'ň' -> "n"
            'ç', 'ć', 'ĉ', 'ċ', 'č' -> "c"
            'ś', 'ŝ', 'ş', 'š' -> "s"
            'ź', 'ż', 'ž' -> "z"
            'ł' -> "l"
            'ğ', 'ĝ', 'ġ', 'ģ' -> "g"
            'ř' -> "r"
            'þ' -> "th"
            'ð' -> "d"
            'ß' -> "ss"
            else -> when {
                char.isLetterOrDigit() -> char.toString()
                char.isWhitespace() -> " "
                else -> " "
            }
        }
        if (normalizedChar == " ") {
            if (!previousWasSpace) append(normalizedChar)
            previousWasSpace = true
        } else {
            append(normalizedChar)
            previousWasSpace = false
        }
    }
}.trim()
