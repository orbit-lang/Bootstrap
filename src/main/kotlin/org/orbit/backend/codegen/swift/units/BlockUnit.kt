package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.AssignmentStatementNode
import org.orbit.core.nodes.BlockNode
import org.orbit.core.nodes.ReturnStatementNode
import org.orbit.util.partial

class ReturnStatementUnit(override val node: ReturnStatementNode, override val depth: Int) : CodeUnit<ReturnStatementNode> {
    override fun generate(mangler: Mangler): String = """
        |return ${RValueUnit(node.valueNode, depth).generate(mangler)}
    """.trimMargin().prependIndent(indent())
}

class AssignmentStatementUnit(override val node: AssignmentStatementNode, override val depth: Int) : CodeUnit<AssignmentStatementNode> {
    override fun generate(mangler: Mangler): String {
        // TODO - Mutability?!
        val rhs = ExpressionUnit(node.value, depth).generate(mangler)

        return "let ${node.identifier.identifier} = $rhs".prependIndent(indent())
    }
}

class BlockUnit(override val node: BlockNode, override val depth: Int) : CodeUnit<BlockNode> {
    override fun generate(mangler: Mangler): String {
        val units: List<CodeUnit<*>> = node.body.mapNotNull {
            when (it) {
                is ReturnStatementNode ->
                    ReturnStatementUnit(it, depth)

                is AssignmentStatementNode ->
                    AssignmentStatementUnit(it, depth)

                else ->
                    TODO("Generate code for statement in block: $it")
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