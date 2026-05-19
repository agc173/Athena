package com.agc.bwitch.domain.security

object InputPolicy {
    const val EMAIL_MAX_LENGTH = 254
    const val ORACLE_QUESTION_MAX_LENGTH = 150
    const val DAILY_RITUAL_TEXT_MAX_LENGTH = 500

    fun normalizeSingleLineInput(input: String, maxLength: Int): String {
        val trimmed = input.trim()
        val withoutControlChars = removeUnsafeControlChars(trimmed, allowNewLines = false)
        val normalizedSpaces = withoutControlChars.replace(Regex("\\s+"), " ")
        return normalizedSpaces.take(maxLength)
    }

    fun normalizeMultilineInput(input: String, maxLength: Int): String {
        val normalizedLineEndings = input.replace("\r\n", "\n").replace('\r', '\n')
        val trimmed = normalizedLineEndings.trim()
        val withoutControlChars = removeUnsafeControlChars(trimmed, allowNewLines = true)
        return withoutControlChars.take(maxLength)
    }

    fun isEmailLengthValid(email: String): Boolean = email.length <= EMAIL_MAX_LENGTH

    fun isNonBlankWithinLimit(text: String, maxLength: Int): Boolean =
        text.isNotBlank() && text.length <= maxLength

    private fun removeUnsafeControlChars(input: String, allowNewLines: Boolean): String = buildString {
        input.forEach { char ->
            val shouldKeep = when {
                allowNewLines && (char == '\n' || char == '\t') -> true
                !allowNewLines && char == '\t' -> true
                char == '\u0000' -> false
                char < ' ' || char == '\u007F' -> false
                else -> true
            }
            if (shouldKeep) append(char)
        }
    }
}
