package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path

object SumTypeEquality : Equality<SumType, Type> {
    override fun isSatisfied(context: ContextProtocol, source: SumType, target: Type): Boolean {
        return source.left.equalitySemantics.isSatisfied(context, source.left, target)
            || source.right.equalitySemantics.isSatisfied(context, source.right, target)
    }
}

class SumType(
    override val name: String,
    val left: Type,
    val right: Type
) : Entity(name, emptyList(), emptyList(), SumTypeEquality, false) {
    constructor(path: Path, left: Type, right: Type) : this(path.toString(OrbitMangler), left, right)

    override val properties: List<Property> = left.properties + right.properties
    override val traitConformance: List<Trait> = left.traitConformance + right.traitConformance
    override val isEphemeral: Boolean = false
    override val equalitySemantics: Equality<out Entity, out Entity> = SumTypeEquality
}