package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

data class TraitConstructor(override val name: String, override val typeParameters: List<TypeParameter>, override val properties: List<Property> = emptyList()) :
    EntityConstructor {
    // TODO - We need a separate 'TraitConstructorEquality' because of method signatures in trait constructors
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = TypeConstructorEquality

    constructor(path: Path, typeParameters: List<TypeParameter> = emptyList(), properties: List<Property> = emptyList()) : this(path.toString(
        OrbitMangler
    ), typeParameters, properties)
}