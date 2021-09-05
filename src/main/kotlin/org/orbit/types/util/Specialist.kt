package org.orbit.types.util

import org.orbit.types.components.TypeProtocol

interface Specialisation<T: TypeProtocol> {
    fun specialise() : T
}

/**
 * EXAMPLE:
 *
 * trait T(x Int)
 * type A : T
 * type B(t T) // (T) -> B<T>
 *
 * b = B(A()) // => b = B(
 */
class TypePropertySpecialisation

class Specialist {
    private val specialisations = mutableListOf<Specialisation<*>>()

    fun register(specialisation: Specialisation<*>) {
        specialisations.add(specialisation)
    }
}