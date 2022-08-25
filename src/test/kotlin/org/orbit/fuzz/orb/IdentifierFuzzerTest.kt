package org.orbit.fuzz.orb

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class IdentifierFuzzerTest {
    @Test
    fun `Always generates valid identifiers`() {
        for (i in IntRange(0, 1000)) {
            val fuzz = IdentifierFuzzer.fuzz()

            assertTrue(fuzz.first() in CharacterSetFuzzer.lowerCase.alphabet)

            for (ch in fuzz) {
                assertTrue(ch in CharacterSetFuzzer.anyCharacterOrDigit.alphabet)
            }
        }
    }
}