package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

interface EntityConstructor : TypeProtocol {
    val typeParameters: List<TypeParameter>
    val properties: List<Property>
    val partiallyResolvedTraitConstructors: List<PartiallyResolvedTraitConstructor>

    fun getTypeParameterOrNull(path: Path) : TypeParameter? {
        return typeParameters.find { OrbitMangler.unmangle(it.name) == path }
    }
}