package com.agc.bwitch.domain.security

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InputPolicyTest {

    @Test
    fun normalizeSingleLine_removesUnsafeControlChars() {
        val input = "he\u0000llo\u0007 world"

        val normalized = InputPolicy.normalizeSingleLineInput(input, 100)

        assertEquals("hello world", normalized)
    }

    @Test
    fun normalizeSingleLine_preservesUnicode() {
        val input = "  mañana ñandú ✨  "

        val normalized = InputPolicy.normalizeSingleLineInput(input, 100)

        assertEquals("mañana ñandú ✨", normalized)
    }

    @Test
    fun normalizeMultiline_preservesNewLines() {
        val input = "\n\nline 1\r\nline 2\u0000\nline\u0007 3\n"

        val normalized = InputPolicy.normalizeMultilineInput(input, 100)

        assertEquals("line 1\nline 2\nline 3", normalized)
    }

    @Test
    fun normalizeMultiline_appliesMaxLength() {
        val normalized = InputPolicy.normalizeMultilineInput("abcdef", 3)

        assertEquals("abc", normalized)
    }

    @Test
    fun normalizeFreeText_preservesTypingSpacesUnicodeAndPunctuation() {
        val input = "  ¿mañana, sí o no? ñandú ✨  "

        val normalized = InputPolicy.normalizeFreeTextInput(input, 100)

        assertEquals("  ¿mañana, sí o no? ñandú ✨  ", normalized)
    }

    @Test
    fun normalizeFreeText_removesUnsafeControlCharsAndAppliesMaxLength() {
        val normalized = InputPolicy.normalizeFreeTextInput("ab\u0000cdef", 4)

        assertEquals("abcd", normalized)
    }

    @Test
    fun emailLengthValidation_worksAtBoundary() {
        assertTrue(InputPolicy.isEmailLengthValid("a".repeat(InputPolicy.EMAIL_MAX_LENGTH)))
        assertFalse(InputPolicy.isEmailLengthValid("a".repeat(InputPolicy.EMAIL_MAX_LENGTH + 1)))
    }
}
