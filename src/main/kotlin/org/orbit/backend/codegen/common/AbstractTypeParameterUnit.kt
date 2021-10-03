package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.TypeIdentifierNode

interface AbstractTypeParameterUnit : CodeUnit<TypeIdentifierNode>

class TypeParameterUnit(override val node: TypeIdentifierNode, override val depth: Int) : AbstractTypeParameterUnit {
    override fun generate(mangler: Mangler): String {
        return node.value
    }
}