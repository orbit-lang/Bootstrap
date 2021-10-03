package org.orbit.backend.codegen.c.units

import org.orbit.backend.codegen.common.AbstractPropertyDefUnit
import org.orbit.core.Mangler
import org.orbit.core.getPath
import org.orbit.core.nodes.PairNode

class PropertyDefUnit(override val node: PairNode, override val depth: Int) : AbstractPropertyDefUnit {
    override fun generate(mangler: Mangler): String {
        val type = node.getPath().toString(mangler)
        val header = "/* ${node.identifierNode.identifier} $type */"

        return """
            |$header
            |$type ${node.identifierNode.identifier};
        """.trimMargin().prependIndent(indent(depth - 1))
    }
}