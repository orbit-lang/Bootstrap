package org.orbit.fuzz.orb

import org.orbit.fuzz.ISourceFuzzer

object TypeDefFuzzer : ISourceFuzzer {
    override fun fuzz(): String {
        val typeName = TypeIdentifierFuzzer.fuzz()

        return "type $typeName"
    }
}