package org.orbit.backend.codegen.common

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.IdentifierNode

interface AbstractIdentifierUnit : CodeUnit<IdentifierNode>

class IdentifierUnit(override val node: IdentifierNode, override val depth: Int) : AbstractIdentifierUnit {
    override fun generate(mangler: Mangler): String {
        return node.identifier
    }
}