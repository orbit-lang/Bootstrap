package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TraitDefNode

data class Trait(
    override val name: String,
    val typeParameters: List<ValuePositionType> = emptyList(),
    override val properties: List<Property> = emptyList(),
    override val traitConformance: List<Trait> = emptyList(),
    val signatures: List<SignatureProtocol<*>> = emptyList(),
    override val equalitySemantics: Equality<Trait, Type> = TraitConformanceEquality,
    val implicit: Boolean = false,
    override val isEphemeral: Boolean = false
) : Entity(name, properties, traitConformance, equalitySemantics) {
    constructor(path: Path, typeParameters: List<ValuePositionType> = emptyList(), properties: List<Property> = emptyList(), traitConformance: List<Trait> = emptyList(), signatures: List<SignatureProtocol<*>> = emptyList(), equalitySemantics: Equality<Trait, Type> = StructuralEquality, implicit: Boolean = false, isEphemeral: Boolean = false)
        : this(path.toString(OrbitMangler), typeParameters, properties, traitConformance, signatures, equalitySemantics, implicit, isEphemeral)

    constructor(node: TraitDefNode) : this(node.getPath())

    override fun equals(other: Any?): Boolean = when (other) {
        is Trait -> name == other.name
        else -> false
    }
}