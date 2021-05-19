package org.orbit.backend.codegen.swift.units

import org.orbit.backend.codegen.CodeUnit
import org.orbit.core.Mangler
import org.orbit.core.nodes.*
import org.orbit.util.partial

class ReturnStatementUnit(override val node: ReturnStatementNode, override val depth: Int, private val resultIsDeferred: Boolean) : CodeUnit<ReturnStatementNode> {
    override fun generate(mangler: Mangler): String {
        val retVal = RValueUnit(node.valueNode, depth).generate(mangler)

        return if (resultIsDeferred) {
            """
            |let __ret_val = $retVal
            |__on_defer(__ret_val)
            |return $retVal
            """.trimMargin()
        } else {
            """
            |return $retVal
            """.trimMargin()
        }.prependIndent(indent())
    }
}

class PrintStatementUnit(override val node: PrintNode, override val depth: Int) : CodeUnit<PrintNode> {
    override fun generate(mangler: Mangler): String {
        val value = ExpressionUnit(node.expressionNode, depth).generate(mangler)

        return "print($value)".prependIndent(indent())
    }
}

class AssignmentStatementUnit(override val node: AssignmentStatementNode, override val depth: Int) : CodeUnit<AssignmentStatementNode> {
    override fun generate(mangler: Mangler): String {
        // TODO - Mutability?!
        val rhs = ExpressionUnit(node.value, depth).generate(mangler)

        return "let ${node.identifier.identifier} = $rhs".prependIndent(indent())
    }
}

class DeferUnit(override val node: DeferNode, override val depth: Int) : CodeUnit<DeferNode> {
    override fun generate(mangler: Mangler): String {
        val block = BlockUnit(node.blockNode, depth, true)
            .generate(mangler)
        val retId = when (node.returnValueIdentifier) {
            null -> ""
            else -> "${node.returnValueIdentifier!!.identifier} in"
        }

        return """
            |let __on_defer = { $retId
            |$block
            |}
        """.trimMargin().prependIndent()
    }
}

class BlockUnit(override val node: BlockNode, override val depth: Int, private val stripBraces: Boolean = false) : CodeUnit<BlockNode> {
    override fun generate(mangler: Mangler): String {
        val units: List<CodeUnit<*>> = node.body.mapNotNull {
            when (it) {
                is ReturnStatementNode ->
                    ReturnStatementUnit(it, depth, true)

                is AssignmentStatementNode ->
                    AssignmentStatementUnit(it, depth)

                is PrintNode -> PrintStatementUnit(it, depth)

                is DeferNode -> DeferUnit(it, depth)

                else ->
                    TODO("Generate code for statement in block: $it")
            }
        }

        val body = units.joinToString("\n|", transform = partial(CodeUnit<*>::generate, mangler))
        return when (stripBraces) {
            true -> body
            else -> """
            |{
            |$body
            |}
            """.trimMargin()
        }
    }
}