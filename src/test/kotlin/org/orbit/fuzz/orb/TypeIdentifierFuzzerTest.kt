package org.orbit.fuzz.orb

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class TypeIdentifierFuzzerTest {

    @Test
    fun `Always generates valid type identifiers`() {
        for (i in IntRange(0, 1000)) {
            val fuzz = TypeIdentifierFuzzer.fuzz()

            assertTrue(fuzz.first() in CharacterSetFuzzer.upperCase.alphabet)

            for (ch in fuzz) {
                assertTrue(ch in CharacterSetFuzzer.any.alphabet)
            }
        }
    }
}