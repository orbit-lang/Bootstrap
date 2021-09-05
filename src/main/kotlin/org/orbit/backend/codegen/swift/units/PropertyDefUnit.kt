package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.getPath
import org.orbit.core.nodes.PairNode

class PropertyDefUnit(override val node: PairNode, override val depth: Int, private val isProtocol: Boolean) :
    CodeUnit<PairNode> {
    override fun generate(mangler: Mangler): String {
        val type = node.getPath().toString(mangler)
        val header = "/* ${node.identifierNode.identifier} $type */"

        return when (isProtocol) {
            true -> """
            |var ${node.identifierNode.identifier} : $type { get }
            """.trimMargin().prependIndent(indent(depth - 1))
            else -> """
            |$header
            |let ${node.identifierNode.identifier} : $type
        """.trimMargin().prependIndent(indent(depth - 1))
        }
    }
}