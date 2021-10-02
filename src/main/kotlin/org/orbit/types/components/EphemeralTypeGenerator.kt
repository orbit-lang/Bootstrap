package org.orbit.types.components

class EphemeralTypeGenerator {
    private var counter = 0

    fun generateEphemeralType(baseName: String, traits: List<Trait>) : Type {
        try {
            return Type("${baseName}::$counter", traitConformance = traits, isEphemeral = true)
        } finally {
            counter += 1
        }
    }
}