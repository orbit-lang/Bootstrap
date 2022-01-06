package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.util.Printer

interface PropertyProvidingType : TypeProtocol {
    val properties: List<Property>
}

abstract class Entity(
    override val name: String,
    override val properties: List<Property> = emptyList(),
    open val traitConformance: List<Trait> = emptyList(),
    override val equalitySemantics: Equality<out Entity, out Entity>,
    override val isEphemeral: Boolean = false,
) : ValuePositionType, PropertyProvidingType, TypeExpression {
    constructor(path: Path, properties: List<Property> = emptyList(), traitConformance: List<Trait> = emptyList(), equalitySemantics: Equality<out Entity, out Entity>, isEphemeral: Boolean = false)
        : this(path.toString(OrbitMangler), properties, traitConformance, equalitySemantics, isEphemeral)

    override val kind: TypeKind = NullaryType

    override fun evaluate(context: ContextProtocol): TypeProtocol = this

    override fun equals(other: Any?): Boolean = when (other) {
        is Entity -> name == other.name
        else -> false
    }
}
