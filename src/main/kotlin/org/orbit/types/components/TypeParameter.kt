package org.orbit.types.components

import org.orbit.core.OrbitMangler
import org.orbit.core.Path
import org.orbit.core.getPath
import org.orbit.core.nodes.TypeIdentifierNode

data class TypeParameter(override val name: String, val constraints: List<Trait> = emptyList()) : VirtualType, ValuePositionType {
    override val equalitySemantics: Equality<out TypeProtocol, out TypeProtocol> = NominalEquality
    override val kind: TypeKind = NullaryType
    override val isEphemeral: Boolean = true

    constructor(path: Path, constraints: List<Trait> = emptyList()) : this(path.toString(OrbitMangler), constraints)
    // TODO - Complex Type Parameter Expressions
    constructor(node: TypeIdentifierNode) : this(node.getPath())

    override fun evaluate(context: ContextProtocol): TypeProtocol = this

    fun asSelfType() : TypeParameter
        = TypeParameter("Self::$name", constraints)

    fun synthesise() : Type {
        // TODO - Type constraints
        val properties = constraints.flatMap(Trait::properties)

        return Type(name, properties = properties, traitConformance = constraints, isEphemeral = true)
    }
}