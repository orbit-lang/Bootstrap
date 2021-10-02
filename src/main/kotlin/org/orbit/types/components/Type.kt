package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeDefNode

data class Type(
    override val name: String,
    val typeParameters: List<ValuePositionType> = emptyList(),
    override val properties: List<Property> = emptyList(),
    override val traitConformance: List<Trait> = emptyList(),
    override val equalitySemantics: Equality<Entity, Entity> = NominalEquality,
    val isRequired: Boolean = false,
    override val isEphemeral: Boolean = false
) : Entity(name, properties, traitConformance, equalitySemantics) {
    constructor(path: Path, typeParameters: List<ValuePositionType> = emptyList(), properties: List<Property> = emptyList(), traitConformance: List<Trait> = emptyList(), equalitySemantics: Equality<Entity, Entity> = NominalEquality, isRequired: Boolean = false, isEphemeral: Boolean = false)
        : this(path.toString(OrbitMangler), typeParameters, properties, traitConformance, equalitySemantics, isRequired, isEphemeral)

    constructor(node: TypeDefNode)
        : this(node.getPath())
}