package org.orbit.backend.codegen.common

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.orbit.backend.codegen.CodeGenFactory
import org.orbit.backend.codegen.CodeUnit
import org.orbit.backend.codegen.c.units.*
import org.orbit.core.CodeGeneratorQualifier
import org.orbit.core.Mangler
import org.orbit.core.injectQualified
import org.orbit.core.nodes.*
import org.orbit.graph.components.Annotations
import org.orbit.graph.components.StringKey
import org.orbit.graph.extensions.getAnnotation
import org.orbit.util.partial

interface AbstractBlockUnit : CodeUnit<BlockNode> {
    val isMethodBody: Boolean
}

class BlockUnit(override val node: BlockNode, override val depth: Int, private val stripBraces: Boolean = false, override val isMethodBody: Boolean) : AbstractBlockUnit, KoinComponent {
    private val codeGeneratorQualifier: CodeGeneratorQualifier by inject()
    private val codeGenFactory: CodeGenFactory<*> by injectQualified(codeGeneratorQualifier)

    override fun generate(mangler: Mangler): String {
        val deferStatements = node.search(DeferNode::class.java)

        val units: List<CodeUnit<*>> = node.body.map {
            when (it) {
                is ReturnStatementNode -> {
                    val deferFunctions = deferStatements.mapNotNull { d -> d.getAnnotation<StringKey>(Annotations.DeferFunction)?.value }
                    codeGenFactory.getReturnStatementUnit(it, depth, node.search(DeferNode::class.java).isNotEmpty(), deferFunctions)
                }

                is AssignmentStatementNode ->
                    codeGenFactory.getAssignmentStatementUnit(it, depth)

                is PrintNode -> codeGenFactory.getPrintStatementUnit(it, depth)
                is DeferNode -> codeGenFactory.getDeferStatementUnit(it, depth)
                is InvokableNode -> codeGenFactory.getCallUnit(it, depth)
                is ExpressionNode -> codeGenFactory.getExpressionUnit(it, depth)

                else -> TODO("Generate code for statement in block: $it")
            }
        }

        // Blocks that defer without returning (i.e implied Unit return type) need to call their defer block here
        val shouldOutputDeferCall = node.containsDefer && !node.containsReturn

        val defer = when (shouldOutputDeferCall) {
            true -> {
                deferStatements.mapNotNull { d -> d.getAnnotation<StringKey>(Annotations.DeferFunction)?.value }
                    .joinToString("\n") { d -> "\t${d.value}();" }
            }
            else -> ""
        }

        val body = units.joinToString("\n|", transform = partial(CodeUnit<*>::generate, mangler))
        return when (stripBraces) {
            true -> body
            else -> """
            |{
            |$body
            |$defer
            |}
            """.trimMargin()
        }
    }
}