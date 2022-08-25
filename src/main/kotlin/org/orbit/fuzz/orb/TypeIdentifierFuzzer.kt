package org.orbit.fuzz.orb

import org.orbit.fuzz.ISourceFuzzer

object TypeIdentifierFuzzer : ISourceFuzzer {
    override fun fuzz(): String {
        val head = CharacterSetFuzzer.upperCase.fuzz()
        val tail = CharacterSetFuzzer.any.fuzz(8)

        return head + tail
    }
}