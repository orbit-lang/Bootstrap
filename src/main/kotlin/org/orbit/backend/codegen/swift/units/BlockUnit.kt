package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.util.partial

class BlockUnit(override val node: BlockNode, override val depth: Int) : CodeUnit<BlockNode> {
    override fun generate(mangler: Mangler): String {
        val units: List<CodeUnit<*>> = node.body.mapNotNull {
            when (it) {
                is ReturnStatementNode ->
                    ReturnStatementUnit(it, depth)
                else -> null // TODO
            }
        }

        val body = units.joinToString("\n|", transform = partial(CodeUnit<*>::generate, mangler))

        return """
            |{
            |$body
            |}
        """.trimMargin()
    }
}