package org.orbit.fuzz.orb

import org.orbit.fuzz.ISourceFuzzer

object IdentifierFuzzer : ISourceFuzzer {
    override fun fuzz(): String {
        val head = CharacterSetFuzzer.lowerCase.fuzz()
        val tail = CharacterSetFuzzer.any.fuzz(8)

        return head + tail
    }
}