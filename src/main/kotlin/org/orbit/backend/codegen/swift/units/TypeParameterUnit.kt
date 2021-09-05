package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.TypeIdentifierNode

class TypeParameterUnit(override val node: TypeIdentifierNode, override val depth: Int) : CodeUnit<TypeIdentifierNode> {
    override fun generate(mangler: Mangler): String {
        return node.value
    }
}